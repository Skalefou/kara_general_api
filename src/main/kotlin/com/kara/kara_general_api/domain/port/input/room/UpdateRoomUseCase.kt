package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId

data class UpdateRoomCommand(
    val id: RoomId,
    val name: String?,
    val street: String?,
    val city: String?,
    val postalCode: String?,
    val country: String?,
)

sealed interface UpdateRoomResult {
    data class Success(val room: Room) : UpdateRoomResult

    data object NotFound : UpdateRoomResult
}

interface UpdateRoomUseCase {
    fun updateRoom(command: UpdateRoomCommand): UpdateRoomResult
}
