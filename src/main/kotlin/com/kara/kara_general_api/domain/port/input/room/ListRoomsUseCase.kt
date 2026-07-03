package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.Room

data class ListRoomsQuery(
    val page: Int = 0,
    val size: Int = 20,
)

data class RoomPage(
    val rooms: List<Room>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
)

interface ListRoomsUseCase {
    fun listRooms(query: ListRoomsQuery): RoomPage
}
