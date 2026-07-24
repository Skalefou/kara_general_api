package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.input.pool.CreateExtensionPoolCommand
import com.kara.kara_general_api.domain.port.input.pool.CreateExtensionPoolResult
import com.kara.kara_general_api.domain.port.input.pool.CreateExtensionPoolUseCase
import com.kara.kara_general_api.domain.port.input.pool.PoolView
import com.kara.kara_general_api.domain.port.output.BookingExtensionRepository
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

@Service
class CreateExtensionPoolService(
    private val bookingExtensionRepository: BookingExtensionRepository,
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val linkTokenGenerator: LinkTokenGenerator,
    private val emailService: EmailService,
) : CreateExtensionPoolUseCase {

    @Transactional
    override fun createForExtension(command: CreateExtensionPoolCommand): CreateExtensionPoolResult {
        val extension =
            bookingExtensionRepository.findById(command.extensionId)
                ?: return CreateExtensionPoolResult.ExtensionNotFound
        if (extension.userId != command.creatorId) return CreateExtensionPoolResult.NotOwner
        if (!extension.isPending()) return CreateExtensionPoolResult.ExtensionNotPending
        if (extension.paymentMode != PaymentMode.SHARED_POT) return CreateExtensionPoolResult.NotSharedPot
        if (poolRepository.findByExtensionId(extension.id) != null) {
            return CreateExtensionPoolResult.PoolAlreadyExists
        }

        if (command.shares.isEmpty() || command.shares.any { it.amount <= BigDecimal.ZERO }) {
            return CreateExtensionPoolResult.InvalidShares
        }
        val total = command.shares.fold(BigDecimal.ZERO) { acc, s -> acc + s.amount }
        if (total.compareTo(extension.price) != 0) {
            return CreateExtensionPoolResult.SharesMismatch(expected = extension.price, actual = total)
        }

        val now = Instant.now()
        if (!extension.expiresAt.isAfter(now)) return CreateExtensionPoolResult.SettlementWindowTooShort

        val pool =
            poolRepository.save(
                Pool.create(
                    bookingId = extension.bookingId,
                    targetAmount = extension.price,
                    currency = extension.currency,
                    deadline = extension.expiresAt,
                    globalLinkToken = linkTokenGenerator.generate(),
                    extensionId = extension.id,
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

        val roomName =
            bookingRepository.findById(extension.bookingId)
                ?.let { roomRepository.findById(it.roomId)?.name }
                ?: "votre réservation"
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

        return CreateExtensionPoolResult.Created(PoolView.of(pool, shares))
    }
}
