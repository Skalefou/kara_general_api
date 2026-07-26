package com.kara.kara_general_api.application.service.stock

import com.kara.kara_general_api.domain.port.input.stock.RemoveRoomStockCommand
import com.kara.kara_general_api.domain.port.input.stock.RemoveRoomStockResult
import com.kara.kara_general_api.domain.port.input.stock.RemoveRoomStockUseCase
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.RoomStockRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RemoveRoomStockService(
    private val roomRepository: RoomRepository,
    private val roomStockRepository: RoomStockRepository,
    private val serverShiftRepository: ServerShiftRepository,
) : RemoveRoomStockUseCase {
    @Transactional
    override fun removeRoomStock(command: RemoveRoomStockCommand): RemoveRoomStockResult {
        roomRepository.findById(command.roomId) ?: return RemoveRoomStockResult.RoomNotFound
        if (!isOnDutyOrAdmin(command.roomId, command.currentUserId, command.isAdmin, serverShiftRepository)) {
            return RemoveRoomStockResult.NotAuthorized
        }
        val removed = roomStockRepository.deleteByRoomIdAndProductId(command.roomId, command.productId)
        return if (removed) RemoveRoomStockResult.Success else RemoveRoomStockResult.NotInStock
    }
}
