package com.kara.kara_general_api.domain.port.input.stock

import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.stock.RoomStockEntry
import com.kara.kara_general_api.domain.model.user.UserId

data class SetRoomStockCommand(
    val roomId: RoomId,
    val productId: ProductId,
    val quantity: Int,
    val currentUserId: UserId,
    val isAdmin: Boolean,
)

sealed interface SetRoomStockResult {
    data class Success(
        val entry: RoomStockEntry,
    ) : SetRoomStockResult

    data object RoomNotFound : SetRoomStockResult

    data object ProductNotFound : SetRoomStockResult

    data object NotAuthorized : SetRoomStockResult
}

interface SetRoomStockUseCase {
    fun setRoomStock(command: SetRoomStockCommand): SetRoomStockResult
}
