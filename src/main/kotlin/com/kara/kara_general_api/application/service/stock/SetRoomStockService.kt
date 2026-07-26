package com.kara.kara_general_api.application.service.stock

import com.kara.kara_general_api.domain.model.stock.RoomStockEntry
import com.kara.kara_general_api.domain.model.stock.RoomStockItem
import com.kara.kara_general_api.domain.port.input.stock.SetRoomStockCommand
import com.kara.kara_general_api.domain.port.input.stock.SetRoomStockResult
import com.kara.kara_general_api.domain.port.input.stock.SetRoomStockUseCase
import com.kara.kara_general_api.domain.port.output.ProductRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.RoomStockRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SetRoomStockService(
    private val roomRepository: RoomRepository,
    private val productRepository: ProductRepository,
    private val roomStockRepository: RoomStockRepository,
    private val serverShiftRepository: ServerShiftRepository,
) : SetRoomStockUseCase {
    @Transactional
    override fun setRoomStock(command: SetRoomStockCommand): SetRoomStockResult {
        roomRepository.findById(command.roomId) ?: return SetRoomStockResult.RoomNotFound
        if (!isOnDutyOrAdmin(command.roomId, command.currentUserId, command.isAdmin, serverShiftRepository)) {
            return SetRoomStockResult.NotAuthorized
        }
        val product = productRepository.findById(command.productId) ?: return SetRoomStockResult.ProductNotFound
        roomStockRepository.upsert(
            RoomStockItem(roomId = command.roomId, productId = command.productId, quantity = command.quantity),
        )
        return SetRoomStockResult.Success(RoomStockEntry(product = product, quantity = command.quantity))
    }
}
