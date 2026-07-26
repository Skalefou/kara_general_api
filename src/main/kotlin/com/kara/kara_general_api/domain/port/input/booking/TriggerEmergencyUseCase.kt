package com.kara.kara_general_api.domain.port.input.booking

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.user.UserId

data class TriggerEmergencyCommand(
    val bookingId: BookingId,
    val currentUserId: UserId,
    val isAdmin: Boolean,
)

sealed interface TriggerEmergencyResult {
    /** [notifiedServers] = nombre de serveurs rattachés notifiés (peut être 0 si aucun n'est affecté). */
    data class Success(
        val notifiedServers: Int,
    ) : TriggerEmergencyResult

    data object BookingNotFound : TriggerEmergencyResult

    /** L'appelant n'est ni le client de la réservation, ni un administrateur. */
    data object NotAuthorized : TriggerEmergencyResult
}

interface TriggerEmergencyUseCase {
    fun triggerEmergency(command: TriggerEmergencyCommand): TriggerEmergencyResult
}
