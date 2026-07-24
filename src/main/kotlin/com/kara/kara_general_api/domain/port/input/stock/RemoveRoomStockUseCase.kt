package com.kara.kara_general_api.domain.port.input.stock

import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId

data class RemoveRoomStockCommand(
    val roomId: RoomId,
    val productId: ProductId,
    val currentUserId: UserId,
    val isAdmin: Boolean,
)

sealed interface RemoveRoomStockResult {
    data object Success : RemoveRoomStockResult

    data object RoomNotFound : RemoveRoomStockResult

    data object NotAuthorized : RemoveRoomStockResult

    data object NotInStock : RemoveRoomStockResult
}

interface RemoveRoomStockUseCase {
    fun removeRoomStock(command: RemoveRoomStockCommand): RemoveRoomStockResult
}
