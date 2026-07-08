package com.kara.kara_general_api.domain.model.room

import com.kara.kara_general_api.domain.model.room.vo.Address
import java.time.Instant

data class Room(
    val id: RoomId,
    val name: String,
    val address: Address,
    val createdAt: Instant,
    val status: RoomStatus = RoomStatus.OPEN,
    val images: List<RoomImage> = emptyList(),
) {
    init {
        require(name.isNotBlank()) { "Le nom de la salle est obligatoire" }
    }

    fun update(name: String, address: Address, status: RoomStatus): Room =
        copy(name = name, address = address, status = status)

    companion object {
        fun create(name: String, address: Address): Room =
            Room(
                id = RoomId.generate(),
                name = name,
                address = address,
                createdAt = Instant.now(),
                status = RoomStatus.OPEN,
            )
    }
}
