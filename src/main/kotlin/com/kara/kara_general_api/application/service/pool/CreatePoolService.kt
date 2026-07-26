package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolCommand
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolResult
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolUseCase
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
import java.time.Instant

/**
 * Crée une cagnotte pour une réservation PENDING en mode sharedPot appartenant au client. Fige le montant
 * cible (= prix total de la réservation), valide l'invariant somme(parts) == cible (reliquat du créateur
 * inclus), génère les liens et envoie les invitations aux participants disposant d'un email.
 */
@Service
class CreatePoolService(
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val linkTokenGenerator: LinkTokenGenerator,
    private val emailService: EmailService,
) : CreatePoolUseCase {
    @Transactional
    override fun create(command: CreatePoolCommand): CreatePoolResult {
        val booking = bookingRepository.findById(command.bookingId) ?: return CreatePoolResult.BookingNotFound
        if (booking.userId != command.creatorId) return CreatePoolResult.NotOwner
        if (booking.status != BookingStatus.PENDING) return CreatePoolResult.BookingNotPending
        if (booking.paymentMode != PaymentMode.SHARED_POT) return CreatePoolResult.NotSharedPot
        if (poolRepository.findByBookingId(booking.id) != null) return CreatePoolResult.PoolAlreadyExists

        if (command.shares.isEmpty() || command.shares.any { it.amount <= BigDecimal.ZERO }) {
            return CreatePoolResult.InvalidShares
        }
        val total = command.shares.fold(BigDecimal.ZERO) { acc, s -> acc + s.amount }
        if (total.compareTo(booking.totalPrice) != 0) {
            return CreatePoolResult.SharesMismatch(expected = booking.totalPrice, actual = total)
        }

        val now = Instant.now()
        val deadline = Pool.defaultDeadline(now, booking.startAt)
        if (!deadline.isAfter(now)) return CreatePoolResult.ReservationTooClose

        val pool =
            poolRepository.save(
                Pool.create(
                    bookingId = booking.id,
                    targetAmount = booking.totalPrice,
                    currency = booking.currency,
                    deadline = deadline,
                    globalLinkToken = linkTokenGenerator.generate(),
                ),
            )

        val shares =
            command.shares.map { input ->
                val email = input.email?.let { Email(it) }
                PoolShare.create(
                    poolId = pool.id,
                    participantName = input.participantName,
                    email = email,
                    amount = input.amount,
                    uniqueLinkToken = if (email != null) linkTokenGenerator.generate() else null,
                    isCreatorShare = input.isCreatorShare,
                )
            }
        poolShareRepository.saveAll(shares)

        val roomName = roomRepository.findById(booking.roomId)?.name ?: "votre réservation"
        shares
            .filter { !it.isCreatorShare && it.email != null && it.uniqueLinkToken != null }
            .forEach { share ->
                emailService.sendPoolInvitation(
                    email = share.email!!,
                    participantName = share.participantName,
                    roomName = roomName,
                    shareLinkToken = share.uniqueLinkToken!!,
                    deadline = pool.deadline,
                )
            }

        return CreatePoolResult.Created(PoolView.of(pool, shares))
    }
}
