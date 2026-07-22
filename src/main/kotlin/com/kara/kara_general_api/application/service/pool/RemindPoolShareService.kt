package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.port.input.pool.RemindPoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.RemindPoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.RemindPoolShareUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.NotificationService
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Relance un participant : renvoie l'invitation par email (et push best-effort si un token est connu). */
@Service
class RemindPoolShareService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val notificationService: NotificationService,
) : RemindPoolShareUseCase {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun remind(command: RemindPoolShareCommand): RemindPoolShareResult {
        val pool = poolRepository.findById(command.poolId) ?: return RemindPoolShareResult.PoolNotFound
        val booking = bookingRepository.findById(pool.bookingId) ?: return RemindPoolShareResult.PoolNotFound
        if (booking.userId != command.requesterId) return RemindPoolShareResult.NotOwner

        val share = poolShareRepository.findById(command.shareId) ?: return RemindPoolShareResult.ShareNotFound
        if (share.poolId != pool.id) return RemindPoolShareResult.ShareNotFound
        if (share.status != PoolShareStatus.PENDING) return RemindPoolShareResult.AlreadyPaid
        val email = share.email ?: return RemindPoolShareResult.NoEmail
        val token = share.uniqueLinkToken ?: return RemindPoolShareResult.NoEmail

        val roomName = roomRepository.findById(booking.roomId)?.name ?: "votre réservation"
        emailService.sendPoolInvitation(
            email = email,
            participantName = share.participantName,
            roomName = roomName,
            shareLinkToken = token,
            deadline = pool.deadline,
        )

        share.payerUserId?.let { payerId ->
            userRepository.findById(payerId)?.fcmToken?.let { fcmToken ->
                runCatching {
                    notificationService.sendPushNotification(
                        token = fcmToken,
                        title = "Rappel : votre part de cagnotte",
                        body = "Il reste une part à régler pour $roomName.",
                        data = mapOf("poolId" to pool.id.value.toString(), "type" to "POOL_REMINDER"),
                    )
                }.onFailure { logger.warn("Failed to send pool reminder push") }
            }
        }

        return RemindPoolShareResult.Reminded
    }
}
