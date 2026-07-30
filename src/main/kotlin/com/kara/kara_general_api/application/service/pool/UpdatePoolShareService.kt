package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.port.input.pool.PoolView
import com.kara.kara_general_api.domain.port.input.pool.UpdatePoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.UpdatePoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.UpdatePoolShareUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PoolLinkBuilder
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Le créateur modifie le montant d'une part non encore payée. L'écart est répercuté sur le reliquat du
 * créateur pour préserver l'invariant somme(parts) == cible. Rejeté si la part visée (ou le reliquat) est
 * déjà autorisée/capturée.
 *
 * Concurrence : la cagnotte est **verrouillée** ([PoolRepository.findByIdForUpdate]) avant lecture des parts,
 * comme dans [PoolSettlementService] — rééquilibrage et règlement ne peuvent pas s'entrelacer.
 */
@Service
class UpdatePoolShareService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val bookingRepository: BookingRepository,
    private val poolLinkBuilder: PoolLinkBuilder,
    private val poolShareHoldReleaser: PoolShareHoldReleaser,
) : UpdatePoolShareUseCase {
    @Transactional
    override fun updateShare(command: UpdatePoolShareCommand): UpdatePoolShareResult {
        // Verrou pessimiste AVANT lecture des parts : sérialise le rééquilibrage avec le règlement.
        val pool = poolRepository.findByIdForUpdate(command.poolId) ?: return UpdatePoolShareResult.PoolNotFound
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

        // Les deux montants changent : les autorisations Stripe en cours qu'elles portent éventuellement ne les
        // couvrent plus (ni à la baisse — capture supérieure au dû — ni à la hausse — capture impossible), donc
        // elles sont libérées et détachées. Les payeurs concernés règlent à nouveau, au bon montant.
        poolShareRepository.save(poolShareHoldReleaser.release(creatorShare).updateAmount(newCreatorAmount))
        poolShareRepository.save(poolShareHoldReleaser.release(target).updateAmount(command.newAmount))

        return UpdatePoolShareResult.Updated(PoolView.of(pool, poolShareRepository.findByPoolId(pool.id), poolLinkBuilder))
    }
}
