package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.port.input.pool.PoolRecapView
import com.kara.kara_general_api.domain.port.input.pool.collectedAmount
import com.kara.kara_general_api.domain.port.input.pool.percentage
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Component

/**
 * Construit le récapitulatif d'une cagnotte (progression + résumé de réservation, plus éventuellement le
 * détail d'une part). Source **unique** de cette vue, partagée par la lecture publique
 * ([GetPoolRecapService]) et par la réconciliation d'une part ([SyncPoolShareService]) : les deux exposent
 * exactement les mêmes montants et le même calcul de progression.
 *
 * Retourne null quand la réservation rattachée est introuvable (cagnotte orpheline).
 */
@Component
class PoolRecapAssembler(
    private val poolShareRepository: PoolShareRepository,
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
) {
    fun assemble(
        pool: Pool,
        share: PoolShare?,
    ): PoolRecapView? {
        val booking = bookingRepository.findById(pool.bookingId) ?: return null
        val room = roomRepository.findById(booking.roomId)
        val shares = poolShareRepository.findByPoolId(pool.id)
        val collected = collectedAmount(shares)
        return PoolRecapView(
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
        )
    }
}
