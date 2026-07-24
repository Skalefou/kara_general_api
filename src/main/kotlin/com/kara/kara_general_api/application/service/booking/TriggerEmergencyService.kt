package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.emergency.EmergencyAlert
import com.kara.kara_general_api.domain.port.input.booking.TriggerEmergencyCommand
import com.kara.kara_general_api.domain.port.input.booking.TriggerEmergencyResult
import com.kara.kara_general_api.domain.port.input.booking.TriggerEmergencyUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.EmergencyEventPublisher
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Déclenche une alerte d'urgence pour une réservation. Autorisé au client de la réservation et aux
 * administrateurs. L'alerte est diffusée à chaque serveur rattaché à la réservation (via son agenda).
 */
@Service
class TriggerEmergencyService(
    private val bookingRepository: BookingRepository,
    private val roomRepository: RoomRepository,
    private val serverShiftRepository: ServerShiftRepository,
    private val emergencyEventPublisher: EmergencyEventPublisher,
) : TriggerEmergencyUseCase {

    override fun triggerEmergency(command: TriggerEmergencyCommand): TriggerEmergencyResult {
        val booking =
            bookingRepository.findById(command.bookingId) ?: return TriggerEmergencyResult.BookingNotFound

        if (!command.isAdmin && command.currentUserId != booking.userId) {
            return TriggerEmergencyResult.NotAuthorized
        }

        val room = roomRepository.findById(booking.roomId)
        val alert =
            EmergencyAlert(
                bookingId = booking.id,
                roomId = booking.roomId,
                roomName = room?.name ?: "Salle inconnue",
                message = "Urgence signalée : un serveur doit entrer dans la salle.",
                triggeredAt = Instant.now(),
            )

        val serverIds =
            serverShiftRepository.findServerIdsAssignedTo(booking.roomId, booking.startAt, booking.endAt)
        serverIds.forEach { serverId -> emergencyEventPublisher.publishEmergency(serverId, alert) }

        return TriggerEmergencyResult.Success(notifiedServers = serverIds.size)
    }
}
