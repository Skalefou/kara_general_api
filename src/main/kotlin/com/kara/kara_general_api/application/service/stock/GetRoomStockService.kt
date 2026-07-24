package com.kara.kara_general_api.application.service.stock

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.stock.GetRoomStockCommand
import com.kara.kara_general_api.domain.port.input.stock.GetRoomStockResult
import com.kara.kara_general_api.domain.port.input.stock.GetRoomStockUseCase
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.RoomStockRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class GetRoomStockService(
    private val roomRepository: RoomRepository,
    private val roomStockRepository: RoomStockRepository,
    private val serverShiftRepository: ServerShiftRepository,
) : GetRoomStockUseCase {

    override fun getRoomStock(command: GetRoomStockCommand): GetRoomStockResult {
        roomRepository.findById(command.roomId) ?: return GetRoomStockResult.RoomNotFound
        if (!isOnDutyOrAdmin(command.roomId, command.currentUserId, command.isAdmin, serverShiftRepository)) {
            return GetRoomStockResult.NotAuthorized
        }
        return GetRoomStockResult.Success(roomStockRepository.findByRoomId(command.roomId))
    }
}

/**
 * Un administrateur gère le stock de n'importe quelle salle ; un serveur uniquement celui d'une salle
 * où l'un de ses créneaux d'agenda couvre l'instant présent (il est de service dans cette salle).
 */
internal fun isOnDutyOrAdmin(
    roomId: RoomId,
    userId: UserId,
    isAdmin: Boolean,
    serverShiftRepository: ServerShiftRepository,
): Boolean {
    if (isAdmin) return true
    val now = Instant.now()
    return serverShiftRepository.findServerIdsAssignedTo(roomId, now, now).contains(userId)
}
