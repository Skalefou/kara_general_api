package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.input.pool.AddPoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.AddPoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.AddPoolShareUseCase
import com.kara.kara_general_api.domain.port.input.pool.PoolView
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.LinkTokenGenerator
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Le créateur ajoute un participant par email. Pour préserver l'invariant somme(parts) == cible, le
 * montant de la nouvelle part est prélevé sur le reliquat du créateur (part PENDING `isCreatorShare`).
 */
@Service
class AddPoolShareService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
    private val linkTokenGenerator: LinkTokenGenerator,
    private val emailService: EmailService,
) : AddPoolShareUseCase {

    @Transactional
    override fun addShare(command: AddPoolShareCommand): AddPoolShareResult {
        val pool = poolRepository.findById(command.poolId) ?: return AddPoolShareResult.PoolNotFound
        val booking = bookingRepository.findById(pool.bookingId) ?: return AddPoolShareResult.PoolNotFound
        if (booking.userId != command.requesterId) return AddPoolShareResult.NotOwner
        if (!pool.isOpen()) return AddPoolShareResult.PoolClosed
        if (command.amount <= BigDecimal.ZERO) return AddPoolShareResult.InvalidShare

        val shares = poolShareRepository.findByPoolId(pool.id)
        val creatorShare = shares.firstOrNull { it.isCreatorShare } ?: return AddPoolShareResult.NoCreatorRemainder
        if (creatorShare.status != PoolShareStatus.PENDING) return AddPoolShareResult.InsufficientRemainder
        val remaining = creatorShare.amount - command.amount
        if (remaining <= BigDecimal.ZERO) return AddPoolShareResult.InsufficientRemainder

        val email = Email(command.email)
        val token = linkTokenGenerator.generate()
        poolShareRepository.save(creatorShare.updateAmount(remaining))
        poolShareRepository.save(
            PoolShare.create(
                poolId = pool.id,
                participantName = command.participantName,
                email = email,
                amount = command.amount,
                uniqueLinkToken = token,
                isCreatorShare = false,
            ),
        )

        val roomName = roomRepository.findById(booking.roomId)?.name ?: "votre réservation"
        emailService.sendPoolInvitation(
            email = email,
            participantName = command.participantName,
            roomName = roomName,
            shareLinkToken = token,
            deadline = pool.deadline,
        )

        return AddPoolShareResult.Added(PoolView.of(pool, poolShareRepository.findByPoolId(pool.id)))
    }
}
