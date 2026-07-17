package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOption

sealed interface GetRoomResult {
    data class Success(val room: Room, val options: List<RoomOption> = emptyList()) : GetRoomResult

    data object NotFound : GetRoomResult
}

interface GetRoomUseCase {
    fun getRoom(id: RoomId): GetRoomResult
}
