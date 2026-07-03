package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId

interface RoomRepository {
    fun save(room: Room): Room

    fun findById(id: RoomId): Room?

    fun findAll(page: Int, size: Int): List<Room>

    fun count(): Long

    fun deleteById(id: RoomId): Boolean
}
