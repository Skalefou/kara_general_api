package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.pool.ListUserPoolsUseCase
import com.kara.kara_general_api.domain.port.input.pool.PoolSummaryView
import com.kara.kara_general_api.domain.port.input.pool.collectedAmount
import com.kara.kara_general_api.domain.port.input.pool.percentage
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Liste les cagnottes de l'utilisateur (créateur ou détenteur d'une part) sous forme de résumés pour
 * l'onglet « Mes événements ». Lecture seule ; l'ordre (échéance décroissante) est fixé par le repository.
 */
@Service
class ListUserPoolsService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
) : ListUserPoolsUseCase {
    @Transactional(readOnly = true)
    override fun listForUser(userId: UserId): List<PoolSummaryView> =
        poolRepository.findByUserInvolvement(userId).mapNotNull { pool ->
            val booking = bookingRepository.findById(pool.bookingId) ?: return@mapNotNull null
            val shares = poolShareRepository.findByPoolId(pool.id)
            val collected = collectedAmount(shares)
            PoolSummaryView(
                poolId = pool.id.value,
                bookingId = pool.bookingId.value,
                roomName = roomRepository.findById(booking.roomId)?.name ?: "Salle",
                startAt = booking.startAt,
                status = pool.status,
                targetAmount = pool.targetAmount,
                collectedAmount = collected,
                currency = pool.currency,
                percentage = percentage(collected, pool.targetAmount),
                deadline = pool.deadline,
                isCreator = booking.userId == userId,
            )
        }
}
