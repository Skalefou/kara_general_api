package com.kara.kara_general_api.domain.port.input.servershift

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.servershift.ServerShift
import com.kara.kara_general_api.domain.model.servershift.ServerShiftId
import java.time.Instant

/**
 * Mise à jour d'un créneau d'agenda. On ne réaffecte pas le serveur ici (un créneau reste rattaché à
 * son serveur) : seuls la salle, les bornes horaires et la note sont modifiables. Chaque champ non-null
 * remplace la valeur existante ; un champ null laisse la valeur inchangée. [clearNote] force la note à null.
 */
data class UpdateServerShiftCommand(
    val id: ServerShiftId,
    val roomId: RoomId?,
    val startAt: Instant?,
    val endAt: Instant?,
    val note: String?,
    val clearNote: Boolean = false,
)

sealed interface UpdateServerShiftResult {
    data class Success(
        val shift: ServerShift,
    ) : UpdateServerShiftResult

    data object NotFound : UpdateServerShiftResult

    data object RoomNotFound : UpdateServerShiftResult

    data object InvalidTimeSlot : UpdateServerShiftResult

    data object SlotUnavailable : UpdateServerShiftResult
}

interface UpdateServerShiftUseCase {
    fun updateServerShift(command: UpdateServerShiftCommand): UpdateServerShiftResult
}
