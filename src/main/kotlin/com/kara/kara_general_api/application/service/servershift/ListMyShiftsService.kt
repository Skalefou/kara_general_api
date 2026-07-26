package com.kara.kara_general_api.application.service.servershift

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.servershift.ServerShiftWithRoom
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.servershift.ListMyShiftsUseCase
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import org.springframework.stereotype.Service

/**
 * Agenda personnel d'un serveur : ses créneaux enrichis du nom et de la ville de la salle. Les salles
 * sont résolues une seule fois chacune (cache local) pour éviter les requêtes redondantes.
 */
@Service
class ListMyShiftsService(
    private val serverShiftRepository: ServerShiftRepository,
    private val roomRepository: RoomRepository,
) : ListMyShiftsUseCase {
    override fun listMyShifts(serverId: UserId): List<ServerShiftWithRoom> {
        val shifts = serverShiftRepository.findAll(serverId = serverId, roomId = null, from = null, to = null)
        val roomCache = mutableMapOf<RoomId, Room?>()
        return shifts.map { shift ->
            val room = roomCache.getOrPut(shift.roomId) { roomRepository.findById(shift.roomId) }
            ServerShiftWithRoom(
                shift = shift,
                roomName = room?.name ?: "Salle inconnue",
                roomCity = room?.address?.city ?: "",
            )
        }
    }
}
