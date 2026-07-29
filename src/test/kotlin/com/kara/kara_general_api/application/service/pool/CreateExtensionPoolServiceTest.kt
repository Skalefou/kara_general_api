package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.BookingExtension
import com.kara.kara_general_api.domain.model.booking.BookingExtensionStatus
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.pool.CreateExtensionPoolCommand
import com.kara.kara_general_api.domain.port.input.pool.CreateExtensionPoolResult
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolShareInput
import com.kara.kara_general_api.domain.port.output.BookingExtensionRepository
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.LinkTokenGenerator
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreateExtensionPoolServiceTest {
    private val bookingExtensionRepository = mockk<BookingExtensionRepository>(relaxed = true)
    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val roomRepository = mockk<RoomRepository>(relaxed = true)
    private val poolRepository = mockk<PoolRepository>(relaxed = true)
    private val poolShareRepository = mockk<PoolShareRepository>(relaxed = true)
    private val linkTokenGenerator = mockk<LinkTokenGenerator>()
    private val emailService = mockk<EmailService>(relaxed = true)
    private val poolLinkBuilder = FakePoolLinkBuilder()
    private val sut =
        CreateExtensionPoolService(
            bookingExtensionRepository,
            bookingRepository,
            roomRepository,
            poolRepository,
            poolShareRepository,
            linkTokenGenerator,
            emailService,
            poolLinkBuilder,
        )

    private val ownerId = UserId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())

    private fun extension(
        status: BookingExtensionStatus = BookingExtensionStatus.PENDING,
        paymentMode: PaymentMode = PaymentMode.SHARED_POT,
    ) = BookingExtension
        .create(
            bookingId = bookingId,
            userId = ownerId,
            additionalMinutes = 60,
            previousEndAt = Instant.now().plus(Duration.ofHours(1)),
            price = BigDecimal("40.00"),
            currency = Currency.EUR,
            paymentMode = paymentMode,
            now = Instant.now(),
        ).copy(status = status)

    private fun command(
        shares: List<CreatePoolShareInput>,
        extensionId: BookingExtension,
    ) = CreateExtensionPoolCommand(
        extensionId = extensionId.id,
        creatorId = ownerId,
        shares = shares,
    )

    @Test
    fun `should open a pool targeting the extension price and bound to the extension`() {
        val extension = extension()
        every { bookingExtensionRepository.findById(extension.id) } returns extension
        every { poolRepository.findByExtensionId(extension.id) } returns null
        every { linkTokenGenerator.generate() } returns "token"
        val saved = slot<Pool>()
        every { poolRepository.save(capture(saved)) } answers { saved.captured }

        val result =
            sut.createForExtension(
                command(
                    listOf(
                        CreatePoolShareInput("Alice", null, BigDecimal("20.00"), isCreatorShare = true),
                        CreatePoolShareInput("Bob", null, BigDecimal("20.00")),
                    ),
                    extension,
                ),
            )

        assertIs<CreateExtensionPoolResult.Created>(result)
        assertEquals(BigDecimal("40.00"), saved.captured.targetAmount)
        assertEquals(extension.id, saved.captured.extensionId)
        assertEquals(extension.expiresAt, saved.captured.deadline)
    }

    @Test
    fun `should refuse when the shares do not add up to the extension price`() {
        val extension = extension()
        every { bookingExtensionRepository.findById(extension.id) } returns extension
        every { poolRepository.findByExtensionId(extension.id) } returns null

        val result =
            sut.createForExtension(
                command(listOf(CreatePoolShareInput("Alice", null, BigDecimal("10.00"))), extension),
            )

        val mismatch = assertIs<CreateExtensionPoolResult.SharesMismatch>(result)
        assertEquals(BigDecimal("40.00"), mismatch.expected)
        assertEquals(BigDecimal("10.00"), mismatch.actual)
    }

    @Test
    fun `should refuse when the extension is settled with a single payment`() {
        val extension = extension(paymentMode = PaymentMode.PAY_ALL)
        every { bookingExtensionRepository.findById(extension.id) } returns extension

        val result =
            sut.createForExtension(
                command(listOf(CreatePoolShareInput("Alice", null, BigDecimal("40.00"))), extension),
            )

        assertEquals(CreateExtensionPoolResult.NotSharedPot, result)
    }

    @Test
    fun `should refuse when a pool already exists for the extension`() {
        val extension = extension()
        every { bookingExtensionRepository.findById(extension.id) } returns extension
        every { poolRepository.findByExtensionId(extension.id) } returns
            Pool.create(bookingId, BigDecimal("40.00"), Currency.EUR, Instant.now(), "t", extension.id)

        val result =
            sut.createForExtension(
                command(listOf(CreatePoolShareInput("Alice", null, BigDecimal("40.00"))), extension),
            )

        assertEquals(CreateExtensionPoolResult.PoolAlreadyExists, result)
    }
}
