package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.booking.UserBooking
import com.kara.kara_general_api.domain.model.booking.UserBookingOption
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.input.booking.ListUserBookingsCommand
import com.kara.kara_general_api.domain.port.input.booking.ListUserBookingsResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListUserBookingsServiceTest {
    private val bookingRepository = mockk<BookingRepository>()
    private val poolRepository = mockk<PoolRepository>()
    private val poolShareRepository = mockk<PoolShareRepository>()
    private val sut = ListUserBookingsService(bookingRepository, poolRepository, poolShareRepository)

    private val userId = UserId(UUID.randomUUID())
    private val roomId = RoomId(UUID.randomUUID())
    private val command = ListUserBookingsCommand(userId)

    private fun booking(
        id: BookingId = BookingId(UUID.randomUUID()),
        startAt: Instant = Instant.parse("2026-08-01T18:00:00Z"),
        status: BookingStatus = BookingStatus.CONFIRMED,
        paymentMode: PaymentMode = PaymentMode.PAY_ALL,
    ) = Booking(
        id = id,
        roomId = roomId,
        userId = userId,
        startAt = startAt,
        endAt = startAt.plusSeconds(3 * 3600),
        numberOfPeople = 8,
        selectedOptionIds = emptyList(),
        totalPrice = BigDecimal("435.00"),
        currency = Currency.EUR,
        status = status,
        createdAt = Instant.parse("2026-07-20T10:00:00Z"),
        expiresAt = Instant.parse("2026-07-20T10:15:00Z"),
        paymentMode = paymentMode,
    )

    private fun record(
        booking: Booking,
        options: List<UserBookingOption> = emptyList(),
    ) = UserBooking(
        booking = booking,
        roomName = "Salle Étoile",
        roomAddress = Address(street = "12 rue de Paris", city = "Lyon", postalCode = "69002", country = "France"),
        options = options,
    )

    private fun pool(
        id: PoolId,
        bookingId: BookingId,
    ) = Pool(
        id = id,
        bookingId = bookingId,
        targetAmount = BigDecimal("435.00"),
        currency = Currency.EUR,
        status = PoolStatus.OPEN,
        deadline = Instant.parse("2026-08-01T16:00:00Z"),
        globalLinkToken = "tok-global",
        createdAt = Instant.parse("2026-07-20T10:00:00Z"),
    )

    private fun share(
        poolId: PoolId,
        name: String,
        amount: String,
        status: PoolShareStatus,
        email: String?,
    ) = PoolShare(
        id = PoolShareId(UUID.randomUUID()),
        poolId = poolId,
        participantName = name,
        email = email?.let { Email(it) },
        amount = BigDecimal(amount),
        status = status,
        stripePaymentIntentId = null,
        uniqueLinkToken = null,
        payerUserId = null,
        isCreatorShare = false,
    )

    @Test
    fun `should assemble the taken options and the inline pool with its named shares`() {
        val booking = booking(paymentMode = PaymentMode.SHARED_POT, status = BookingStatus.PENDING)
        val optionId = RoomOptionId(UUID.randomUUID())
        val poolId = PoolId(UUID.randomUUID())
        every { bookingRepository.findByUserId(userId) } returns
            listOf(
                record(
                    booking,
                    listOf(
                        UserBookingOption(
                            optionId = optionId,
                            label = "Ménage fin de soirée",
                            price = BigDecimal("60.00"),
                            currency = Currency.EUR,
                        ),
                    ),
                ),
            )
        every { poolRepository.findByBookingIds(listOf(booking.id)) } returns listOf(pool(poolId, booking.id))
        every { poolShareRepository.findByPoolIds(listOf(poolId)) } returns
            listOf(
                share(poolId, "Jeanne Martin", "217.50", PoolShareStatus.AUTHORIZED, "jeanne@example.com"),
                share(poolId, "Karim Belkacem", "217.50", PoolShareStatus.PENDING, null),
            )

        val result = sut.listForUser(command) as ListUserBookingsResult.Success

        assertEquals(1, result.bookings.size)
        val view = result.bookings.first()
        assertEquals("Salle Étoile", view.roomName)
        assertEquals("12 rue de Paris, 69002 Lyon, France", view.roomAddress)
        assertEquals(8, view.numberOfPeople)
        assertEquals(listOf(optionId.value), view.options.map { it.optionId })
        assertEquals("Ménage fin de soirée", view.options.first().label)
        assertEquals(BigDecimal("60.00"), view.options.first().price)
        val pool = requireNotNull(view.pool)
        assertEquals(poolId.value, pool.poolId)
        assertEquals(PoolStatus.OPEN, pool.status)
        assertEquals(BigDecimal("435.00"), pool.targetAmount)
        // Seule la part AUTHORIZED est engagée : 217.50 / 435.00 = 50 %.
        assertEquals(BigDecimal("217.50"), pool.collectedAmount)
        assertEquals(50, pool.percentage)
        assertEquals(listOf("Jeanne Martin", "Karim Belkacem"), pool.shares.map { it.participantName })
        assertEquals(listOf("jeanne@example.com", null), pool.shares.map { it.email })
    }

    @Test
    fun `should expose a null pool when the booking is paid in full by its owner`() {
        val booking = booking(paymentMode = PaymentMode.PAY_ALL)
        every { bookingRepository.findByUserId(userId) } returns listOf(record(booking))
        every { poolRepository.findByBookingIds(listOf(booking.id)) } returns emptyList()
        every { poolShareRepository.findByPoolIds(emptyList()) } returns emptyList()

        val result = sut.listForUser(command) as ListUserBookingsResult.Success

        assertEquals(1, result.bookings.size)
        assertEquals(PaymentMode.PAY_ALL, result.bookings.first().paymentMode)
        assertNull(result.bookings.first().pool)
    }

    @Test
    fun `should return an empty list and query no pool when the user has no booking`() {
        every { bookingRepository.findByUserId(userId) } returns emptyList()

        val result = sut.listForUser(command) as ListUserBookingsResult.Success

        assertTrue(result.bookings.isEmpty())
        verify(exactly = 0) { poolRepository.findByBookingIds(any()) }
        verify(exactly = 0) { poolShareRepository.findByPoolIds(any()) }
    }

    @Test
    fun `should order the bookings by start date descending`() {
        val oldest = booking(startAt = Instant.parse("2026-06-01T18:00:00Z"))
        val newest = booking(startAt = Instant.parse("2026-09-01T18:00:00Z"))
        val middle = booking(startAt = Instant.parse("2026-07-01T18:00:00Z"))
        every { bookingRepository.findByUserId(userId) } returns
            listOf(record(oldest), record(newest), record(middle))
        every { poolRepository.findByBookingIds(any()) } returns emptyList()
        every { poolShareRepository.findByPoolIds(emptyList()) } returns emptyList()

        val result = sut.listForUser(command) as ListUserBookingsResult.Success

        assertEquals(
            listOf(newest.id.value, middle.id.value, oldest.id.value),
            result.bookings.map { it.bookingId },
        )
    }

    @Test
    fun `should return every status without filtering`() {
        val statuses = BookingStatus.entries
        every { bookingRepository.findByUserId(userId) } returns
            statuses.mapIndexed { index, status ->
                record(booking(startAt = Instant.parse("2026-08-0${index + 1}T18:00:00Z"), status = status))
            }
        every { poolRepository.findByBookingIds(any()) } returns emptyList()
        every { poolShareRepository.findByPoolIds(emptyList()) } returns emptyList()

        val result = sut.listForUser(command) as ListUserBookingsResult.Success

        assertEquals(statuses.toSet(), result.bookings.map { it.status }.toSet())
    }
}
