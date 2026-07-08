package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomImage
import com.kara.kara_general_api.domain.model.room.RoomImageId

interface RoomRepository {
    fun save(room: Room): Room

    fun findById(id: RoomId): Room?

    fun findAll(page: Int, size: Int): List<Room>

    fun count(): Long

    fun deleteById(id: RoomId): Boolean

    /** Persiste une image rattachée à une salle. */
    fun addImage(roomId: RoomId, image: RoomImage): RoomImage

    /** Supprime une image d'une salle. Retourne true si une ligne a été supprimée. */
    fun removeImage(roomId: RoomId, imageId: RoomImageId): Boolean
}
