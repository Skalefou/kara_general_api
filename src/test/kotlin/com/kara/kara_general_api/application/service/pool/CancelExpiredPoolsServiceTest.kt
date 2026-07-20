package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentGateway
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

class CancelExpiredPoolsServiceTest {

    private val poolRepository = mockk<PoolRepository>(relaxed = true)
    private val poolShareRepository = mockk<PoolShareRepository>(relaxed = true)
    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val paymentGateway = mockk<PaymentGateway>(relaxed = true)
    private val poolNotifier = mockk<PoolNotifier>(relaxed = true)
    private val sut =
        CancelExpiredPoolsService(poolRepository, poolShareRepository, bookingRepository, paymentGateway, poolNotifier)

    private val poolId = PoolId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())

    @Test
    fun `returns zero and does nothing when there is no expired pool`() {
        every { poolRepository.findExpiredOpen(any()) } returns emptyList()

        assertEquals(0, sut.cancelExpired(Instant.now()))
        verify(exactly = 0) { paymentGateway.cancelPaymentIntent(any()) }
    }

    @Test
    fun `cancels authorizations, expires the pool, cancels the booking and notifies`() {
        val pool = Pool(poolId, bookingId, BigDecimal("100.00"), Currency.EUR, PoolStatus.OPEN, Instant.now().minusSeconds(1), "g", Instant.now())
        val authorizedShare =
            PoolShare(PoolShareId(UUID.randomUUID()), poolId, "A", null, BigDecimal("50.00"), PoolShareStatus.AUTHORIZED, "pi_a", null, null, false)
        val pendingNoIntent =
            PoolShare(PoolShareId(UUID.randomUUID()), poolId, "B", null, BigDecimal("50.00"), PoolShareStatus.PENDING, null, null, null, false)
        every { poolRepository.findExpiredOpen(any()) } returns listOf(pool)
        every { poolShareRepository.findByPoolId(poolId) } returns listOf(authorizedShare, pendingNoIntent)
        every { bookingRepository.findById(bookingId) } returns mockk<Booking>()

        val count = sut.cancelExpired(Instant.now())

        assertEquals(1, count)
        verify(exactly = 1) { paymentGateway.cancelPaymentIntent("pi_a") }
        verify { poolShareRepository.save(match { it.status == PoolShareStatus.CANCELLED }) }
        verify { poolRepository.updateStatus(poolId, PoolStatus.EXPIRED) }
        verify { bookingRepository.updateStatus(bookingId, BookingStatus.CANCELLED) }
        verify { poolNotifier.notifyPoolCancelled(any(), any()) }
    }
}
