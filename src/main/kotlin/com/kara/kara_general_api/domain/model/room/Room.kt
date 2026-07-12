package com.kara.kara_general_api.domain.model.room

import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.room.vo.Coordinates
import java.time.Instant

data class Room(
    val id: RoomId,
    val name: String,
    val address: Address,
    val createdAt: Instant,
    val status: RoomStatus = RoomStatus.OPEN,
    val images: List<RoomImage> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    init {
        require(name.isNotBlank()) { "Le nom de la salle est obligatoire" }
    }

    fun update(name: String, address: Address, status: RoomStatus, coordinates: Coordinates?): Room =
        copy(
            name = name,
            address = address,
            status = status,
            latitude = coordinates?.latitude,
            longitude = coordinates?.longitude,
        )

    companion object {
        fun create(name: String, address: Address, coordinates: Coordinates): Room =
            Room(
                id = RoomId.generate(),
                name = name,
                address = address,
                createdAt = Instant.now(),
                status = RoomStatus.OPEN,
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
            )
    }
}
