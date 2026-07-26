package com.kara.kara_general_api.domain.port.input.stock

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.stock.RoomStockEntry
import com.kara.kara_general_api.domain.model.user.UserId

data class GetRoomStockCommand(
    val roomId: RoomId,
    val currentUserId: UserId,
    val isAdmin: Boolean,
)

sealed interface GetRoomStockResult {
    data class Success(
        val entries: List<RoomStockEntry>,
    ) : GetRoomStockResult

    data object RoomNotFound : GetRoomStockResult

    data object NotAuthorized : GetRoomStockResult
}

interface GetRoomStockUseCase {
    fun getRoomStock(command: GetRoomStockCommand): GetRoomStockResult
}
