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
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListUserPoolsServiceTest {
    private val poolRepository = mockk<PoolRepository>()
    private val poolShareRepository = mockk<PoolShareRepository>()
    private val bookingRepository = mockk<BookingRepository>()
    private val roomRepository = mockk<RoomRepository>()
    private val sut = ListUserPoolsService(poolRepository, poolShareRepository, bookingRepository, roomRepository)

    private val userId = UserId(UUID.randomUUID())

    private fun pool(
        id: PoolId,
        bookingId: BookingId,
    ) = Pool(id, bookingId, BigDecimal("100.00"), Currency.EUR, PoolStatus.OPEN, Instant.now().plusSeconds(3600), "g", Instant.now())

    private fun bookingOwnedBy(
        owner: UserId,
        roomId: RoomId,
    ): Booking {
        val booking = mockk<Booking>()
        every { booking.userId } returns owner
        every { booking.roomId } returns roomId
        every { booking.startAt } returns Instant.parse("2026-08-01T18:00:00Z")
        return booking
    }

    @Test
    fun `returns an empty list when the user is involved in no pool`() {
        every { poolRepository.findByUserInvolvement(userId) } returns emptyList()

        assertTrue(sut.listForUser(userId).isEmpty())
    }

    @Test
    fun `builds a summary flagged isCreator with the collected amount`() {
        val poolId = PoolId(UUID.randomUUID())
        val bookingId = BookingId(UUID.randomUUID())
        val roomId = RoomId(UUID.randomUUID())
        every { poolRepository.findByUserInvolvement(userId) } returns listOf(pool(poolId, bookingId))
        every { bookingRepository.findById(bookingId) } returns bookingOwnedBy(userId, roomId)
        val room = mockk<Room>()
        every { room.name } returns "Salle Étoile"
        every { roomRepository.findById(roomId) } returns room
        every { poolShareRepository.findByPoolId(poolId) } returns
            listOf(
                PoolShare(
                    PoolShareId(UUID.randomUUID()),
                    poolId,
                    "A",
                    null,
                    BigDecimal("40.00"),
                    PoolShareStatus.AUTHORIZED,
                    "pi_a",
                    null,
                    null,
                    false,
                ),
                PoolShare(
                    PoolShareId(UUID.randomUUID()),
                    poolId,
                    "B",
                    null,
                    BigDecimal("60.00"),
                    PoolShareStatus.PENDING,
                    null,
                    null,
                    null,
                    true,
                ),
            )

        val summaries = sut.listForUser(userId)

        assertEquals(1, summaries.size)
        val s = summaries.first()
        assertEquals("Salle Étoile", s.roomName)
        assertTrue(s.isCreator)
        assertEquals(BigDecimal("40.00"), s.collectedAmount)
        assertEquals(40, s.percentage)
    }

    @Test
    fun `flags isCreator false when the user only holds a share`() {
        val poolId = PoolId(UUID.randomUUID())
        val bookingId = BookingId(UUID.randomUUID())
        val roomId = RoomId(UUID.randomUUID())
        every { poolRepository.findByUserInvolvement(userId) } returns listOf(pool(poolId, bookingId))
        every { bookingRepository.findById(bookingId) } returns bookingOwnedBy(UserId(UUID.randomUUID()), roomId)
        every { roomRepository.findById(roomId) } returns null
        every { poolShareRepository.findByPoolId(poolId) } returns emptyList()

        val summaries = sut.listForUser(userId)

        assertEquals(1, summaries.size)
        assertEquals("Salle", summaries.first().roomName)
        assertTrue(!summaries.first().isCreator)
    }
}
