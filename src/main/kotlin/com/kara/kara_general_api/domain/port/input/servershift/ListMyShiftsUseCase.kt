package com.kara.kara_general_api.domain.port.input.servershift

import com.kara.kara_general_api.domain.model.servershift.ServerShiftWithRoom
import com.kara.kara_general_api.domain.model.user.UserId

/**
 * Agenda personnel d'un serveur : ses propres créneaux (salles où il doit se rendre), ordonnés par date.
 */
interface ListMyShiftsUseCase {
    fun listMyShifts(serverId: UserId): List<ServerShiftWithRoom>
}
