package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.port.input.pool.PoolView
import com.kara.kara_general_api.domain.port.input.pool.UpdatePoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.UpdatePoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.UpdatePoolShareUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Le créateur modifie le montant d'une part non encore payée. L'écart est répercuté sur le reliquat du
 * créateur pour préserver l'invariant somme(parts) == cible. Rejeté si la part visée (ou le reliquat) est
 * déjà autorisée/capturée.
 */
@Service
class UpdatePoolShareService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val bookingRepository: BookingRepository,
) : UpdatePoolShareUseCase {
    @Transactional
    override fun updateShare(command: UpdatePoolShareCommand): UpdatePoolShareResult {
        val pool = poolRepository.findById(command.poolId) ?: return UpdatePoolShareResult.PoolNotFound
        val booking = bookingRepository.findById(pool.bookingId) ?: return UpdatePoolShareResult.PoolNotFound
        if (booking.userId != command.requesterId) return UpdatePoolShareResult.NotOwner
        if (!pool.isOpen()) return UpdatePoolShareResult.PoolClosed
        if (command.newAmount <= BigDecimal.ZERO) return UpdatePoolShareResult.InvalidAmount

        val target = poolShareRepository.findById(command.shareId) ?: return UpdatePoolShareResult.ShareNotFound
        if (target.poolId != pool.id) return UpdatePoolShareResult.ShareNotFound
        if (target.isCreatorShare) return UpdatePoolShareResult.CannotEditCreatorShare
        if (target.status != PoolShareStatus.PENDING) return UpdatePoolShareResult.ShareAlreadyPaid

        val shares = poolShareRepository.findByPoolId(pool.id)
        val creatorShare =
            shares.firstOrNull { it.isCreatorShare } ?: return UpdatePoolShareResult.InsufficientRemainder
        if (creatorShare.status != PoolShareStatus.PENDING) return UpdatePoolShareResult.CreatorShareLocked

        val delta = command.newAmount - target.amount
        val newCreatorAmount = creatorShare.amount - delta
        if (newCreatorAmount <= BigDecimal.ZERO) return UpdatePoolShareResult.InsufficientRemainder

        poolShareRepository.save(creatorShare.updateAmount(newCreatorAmount))
        poolShareRepository.save(target.updateAmount(command.newAmount))

        return UpdatePoolShareResult.Updated(PoolView.of(pool, poolShareRepository.findByPoolId(pool.id)))
    }
}
