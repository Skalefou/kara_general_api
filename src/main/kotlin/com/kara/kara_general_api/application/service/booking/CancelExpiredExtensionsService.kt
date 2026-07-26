package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.application.service.pool.PoolSettlementService
import com.kara.kara_general_api.domain.model.booking.BookingExtensionStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.port.input.booking.CancelExpiredExtensionsUseCase
import com.kara.kara_general_api.domain.port.output.BookingExtensionRepository
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CancelExpiredExtensionsService(
    private val bookingExtensionRepository: BookingExtensionRepository,
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val poolSettlementService: PoolSettlementService,
) : CancelExpiredExtensionsUseCase {
    @Transactional
    override fun cancelExpired(now: Instant): Int {
        val expired = bookingExtensionRepository.findExpiredPending(now)
        expired.forEach { extension ->
            poolRepository.findByExtensionId(extension.id)?.let { pool ->
                if (pool.isOpen()) {
                    poolSettlementService.cancelShareHolds(poolShareRepository.findByPoolId(pool.id))
                    poolRepository.updateStatus(pool.id, PoolStatus.EXPIRED)
                }
            }
            bookingExtensionRepository.updateStatus(extension.id, BookingExtensionStatus.CANCELLED)
        }
        return expired.size
    }
}
