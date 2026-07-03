package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId

sealed interface GetRoomResult {
    data class Success(val room: Room) : GetRoomResult

    data object NotFound : GetRoomResult
}

interface GetRoomUseCase {
    fun getRoom(id: RoomId): GetRoomResult
}
