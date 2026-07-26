package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.input.pool.AddPoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.AddPoolShareResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.LinkTokenGenerator
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AddPoolShareServiceTest {
    private val poolRepository = mockk<PoolRepository>()
    private val poolShareRepository = mockk<PoolShareRepository>(relaxed = true)
    private val bookingRepository = mockk<BookingRepository>()
    private val roomRepository = mockk<RoomRepository>()
    private val linkTokenGenerator = mockk<LinkTokenGenerator>()
    private val emailService = mockk<EmailService>(relaxed = true)
    private val sut =
        AddPoolShareService(
            poolRepository,
            poolShareRepository,
            bookingRepository,
            roomRepository,
            linkTokenGenerator,
            emailService,
        )

    private val poolId = PoolId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())
    private val creatorId = UserId(UUID.randomUUID())

    private fun pool(status: PoolStatus = PoolStatus.OPEN) =
        Pool(poolId, bookingId, BigDecimal("100.00"), Currency.EUR, status, Instant.now().plusSeconds(3600), "g", Instant.now())

    private fun creatorShare(
        amount: String = "100.00",
        status: PoolShareStatus = PoolShareStatus.PENDING,
    ) = PoolShare(PoolShareId(UUID.randomUUID()), poolId, "Créateur", null, BigDecimal(amount), status, null, null, creatorId, true)

    private fun stubBookingOwnedBy(owner: UserId) {
        val booking = mockk<Booking>()
        every { booking.userId } returns owner
        every { booking.roomId } returns RoomId(UUID.randomUUID())
        every { bookingRepository.findById(bookingId) } returns booking
    }

    private fun command(amount: String = "40.00") = AddPoolShareCommand(poolId, creatorId, "Bob", "bob@example.com", BigDecimal(amount))

    @Test
    fun `returns NotOwner when the caller does not own the booking`() {
        every { poolRepository.findById(poolId) } returns pool()
        stubBookingOwnedBy(UserId(UUID.randomUUID()))

        assertEquals(AddPoolShareResult.NotOwner, sut.addShare(command()))
    }

    @Test
    fun `returns InsufficientRemainder when the creator remainder cannot fund the new share`() {
        every { poolRepository.findById(poolId) } returns pool()
        stubBookingOwnedBy(creatorId)
        every { poolShareRepository.findByPoolId(poolId) } returns listOf(creatorShare(amount = "30.00"))

        assertEquals(AddPoolShareResult.InsufficientRemainder, sut.addShare(command(amount = "40.00")))
    }

    @Test
    fun `carves the new share from the creator remainder and sends an invitation`() {
        every { poolRepository.findById(poolId) } returns pool()
        stubBookingOwnedBy(creatorId)
        every { poolShareRepository.findByPoolId(poolId) } returnsMany
            listOf(
                listOf(creatorShare(amount = "100.00")),
                listOf(creatorShare(amount = "60.00")),
            )
        every { linkTokenGenerator.generate() } returns "tok"
        val room = mockk<Room>()
        every { room.name } returns "Salle"
        every { roomRepository.findById(any()) } returns room

        val result = sut.addShare(command(amount = "40.00"))

        assertIs<AddPoolShareResult.Added>(result)
        // Creator remainder reduced to 60, and a new 40 share created.
        verify { poolShareRepository.save(match { it.isCreatorShare && it.amount == BigDecimal("60.00") }) }
        verify { poolShareRepository.save(match { !it.isCreatorShare && it.amount == BigDecimal("40.00") }) }
        verify(exactly = 1) { emailService.sendPoolInvitation(Email("bob@example.com"), "Bob", "Salle", "tok", any()) }
    }
}
