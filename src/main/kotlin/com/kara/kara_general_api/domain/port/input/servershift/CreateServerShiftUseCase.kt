package com.kara.kara_general_api.domain.port.input.servershift

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.servershift.ServerShift
import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant

data class CreateServerShiftCommand(
    val serverId: UserId,
    val roomId: RoomId,
    val startAt: Instant,
    val endAt: Instant,
    val note: String?,
)

sealed interface CreateServerShiftResult {
    data class Created(
        val shift: ServerShift,
    ) : CreateServerShiftResult

    /** Aucun compte ne correspond à [CreateServerShiftCommand.serverId]. */
    data object ServerNotFound : CreateServerShiftResult

    /** Le compte visé existe mais n'a pas le rôle SERVER. */
    data object NotAServer : CreateServerShiftResult

    data object RoomNotFound : CreateServerShiftResult

    /** [CreateServerShiftCommand.endAt] n'est pas strictement postérieur à [CreateServerShiftCommand.startAt]. */
    data object InvalidTimeSlot : CreateServerShiftResult

    /** Le créneau chevauche un autre créneau du même serveur. */
    data object SlotUnavailable : CreateServerShiftResult
}

interface CreateServerShiftUseCase {
    fun createServerShift(command: CreateServerShiftCommand): CreateServerShiftResult
}
