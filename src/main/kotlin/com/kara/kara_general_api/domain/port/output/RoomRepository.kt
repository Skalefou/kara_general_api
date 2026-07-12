package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomImage
import com.kara.kara_general_api.domain.model.room.RoomImageId
import com.kara.kara_general_api.domain.model.room.vo.BoundingBox

interface RoomRepository {
    fun save(room: Room): Room

    fun findById(id: RoomId): Room?

    fun findAll(page: Int, size: Int): List<Room>

    fun count(): Long

    /** Salles dont les coordonnées tombent dans la bbox, plafonnées à [limit]. */
    fun findInBbox(bbox: BoundingBox, limit: Int): List<Room>

    /** Nombre réel de salles dans la bbox, avant plafonnement. */
    fun countInBbox(bbox: BoundingBox): Long

    fun deleteById(id: RoomId): Boolean

    /** Persiste une image rattachée à une salle. */
    fun addImage(roomId: RoomId, image: RoomImage): RoomImage

    /** Supprime une image d'une salle. Retourne true si une ligne a été supprimée. */
    fun removeImage(roomId: RoomId, imageId: RoomImageId): Boolean
}
