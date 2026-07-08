package com.kara.kara_general_api.domain.model.room

import java.util.UUID

@JvmInline
value class RoomImageId(val value: UUID) {
    companion object {
        fun generate(): RoomImageId = RoomImageId(UUID.randomUUID())
    }
}

/**
 * Image d'une salle, publique. [objectKey] est la clé de l'objet stocké dans le bucket public ;
 * l'URL publique (CDN) est calculée par l'adaptateur de stockage, jamais par le domaine.
 */
data class RoomImage(
    val id: RoomImageId,
    val objectKey: String,
    val position: Int,
)
