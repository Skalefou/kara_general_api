package com.kara.kara_general_api.domain.port.input.servershift

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.servershift.ServerShift
import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant

/**
 * Consultation de l'agenda de tous les serveurs. Sans filtre, retourne l'ensemble des créneaux ;
 * [serverId] restreint à un serveur, [roomId] à une salle, [from]/[to] à une fenêtre temporelle.
 */
data class ListServerShiftsQuery(
    val serverId: UserId? = null,
    val roomId: RoomId? = null,
    val from: Instant? = null,
    val to: Instant? = null,
)

interface ListServerShiftsUseCase {
    fun listServerShifts(query: ListServerShiftsQuery): List<ServerShift>
}
