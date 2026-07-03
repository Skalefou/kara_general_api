package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.RoomId

sealed interface DeleteRoomResult {
    data object Success : DeleteRoomResult

    data object NotFound : DeleteRoomResult
}

interface DeleteRoomUseCase {
    fun deleteRoom(id: RoomId): DeleteRoomResult
}
