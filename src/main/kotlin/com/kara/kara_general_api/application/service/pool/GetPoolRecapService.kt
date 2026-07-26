package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.port.input.pool.GetPoolRecapResult
import com.kara.kara_general_api.domain.port.input.pool.GetPoolRecapUseCase
import com.kara.kara_general_api.domain.port.input.pool.PoolRecapView
import com.kara.kara_general_api.domain.port.input.pool.collectedAmount
import com.kara.kara_general_api.domain.port.input.pool.percentage
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Récapitulatif public d'une cagnotte (lecture sans authentification). */
@Service
class GetPoolRecapService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
) : GetPoolRecapUseCase {
    @Transactional(readOnly = true)
    override fun getByGlobalToken(globalToken: String): GetPoolRecapResult {
        val pool = poolRepository.findByGlobalLinkToken(globalToken) ?: return GetPoolRecapResult.NotFound
        return recap(pool, share = null)
    }

    @Transactional(readOnly = true)
    override fun getByShareToken(shareToken: String): GetPoolRecapResult {
        val share = poolShareRepository.findByUniqueLinkToken(shareToken) ?: return GetPoolRecapResult.NotFound
        val pool = poolRepository.findById(share.poolId) ?: return GetPoolRecapResult.NotFound
        return recap(pool, share = share)
    }

    private fun recap(
        pool: Pool,
        share: PoolShare?,
    ): GetPoolRecapResult {
        val booking = bookingRepository.findById(pool.bookingId) ?: return GetPoolRecapResult.NotFound
        val room = roomRepository.findById(booking.roomId)
        val shares = poolShareRepository.findByPoolId(pool.id)
        val collected = collectedAmount(shares)
        return GetPoolRecapResult.Found(
            PoolRecapView(
                poolId = pool.id.value,
                status = pool.status,
                roomName = room?.name ?: "Salle",
                startAt = booking.startAt,
                endAt = booking.endAt,
                numberOfPeople = booking.numberOfPeople,
                targetAmount = pool.targetAmount,
                collectedAmount = collected,
                currency = pool.currency,
                percentage = percentage(collected, pool.targetAmount),
                deadline = pool.deadline,
                shareId = share?.id?.value,
                shareParticipantName = share?.participantName,
                shareAmount = share?.amount,
                shareStatus = share?.status,
            ),
        )
    }
}
