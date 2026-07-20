package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.port.output.BookingRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class CancelExpiredBookingsServiceTest {

    private val bookingRepository = mockk<BookingRepository>()
    private val sut = CancelExpiredBookingsService(bookingRepository)

    @Test
    fun `should delegate to the repository and return the cancelled count`() {
        val now = Instant.parse("2026-07-20T10:20:00Z")
        every { bookingRepository.cancelExpiredPending(now) } returns 3

        val cancelled = sut.cancelExpired(now)

        assertEquals(3, cancelled)
        verify(exactly = 1) { bookingRepository.cancelExpiredPending(now) }
    }
}
