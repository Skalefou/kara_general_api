package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.port.input.pool.CancelExpiredPoolsUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Balaie les cagnottes OPEN dont le délai est échu : annule toutes les autorisations Stripe (zéro
 * prélèvement, conforme au use-case §67), passe la cagnotte EXPIRED et la réservation CANCELLED, puis
 * notifie les participants. La libération des autorisations est déléguée à [PoolSettlementService] (source
 * unique de la logique d'annulation des blocages).
 */
@Service
class CancelExpiredPoolsService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val bookingRepository: BookingRepository,
    private val poolSettlementService: PoolSettlementService,
    private val poolNotifier: PoolNotifier,
) : CancelExpiredPoolsUseCase {

    @Transactional
    override fun cancelExpired(now: Instant): Int {
        val expiredPools = poolRepository.findExpiredOpen(now)
        expiredPools.forEach { pool ->
            val shares = poolShareRepository.findByPoolId(pool.id)
            poolSettlementService.cancelShareHolds(shares)
            poolRepository.updateStatus(pool.id, PoolStatus.EXPIRED)
            bookingRepository.updateStatus(pool.bookingId, BookingStatus.CANCELLED)
            bookingRepository.findById(pool.bookingId)?.let { poolNotifier.notifyPoolCancelled(it, shares) }
        }
        return expiredPools.size
    }
}
