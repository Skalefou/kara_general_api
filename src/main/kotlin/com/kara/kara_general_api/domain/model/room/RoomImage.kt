package com.kara.kara_general_api.domain.model.room

import java.util.UUID

@JvmInline
value class RoomImageId(
    val value: UUID,
) {
    companion object {
        fun generate(): RoomImageId = RoomImageId(UUID.randomUUID())
    }
}

/** Étape du cycle de vie d'une image de salle traitée de façon asynchrone par le worker. */
enum class RoomImageStatus {
    /** Original téléversé, variantes en cours de génération par le worker. */
    PROCESSING,

    /** Variantes générées et persistées : l'image est affichable. */
    READY,

    /** Le worker a échoué : [RoomImage.errorCode] porte la cause. */
    FAILED,
}

/**
 * Variante d'une image de salle produite par le worker (thumbnail / detail / full). [objectKey] est la clé
 * de l'objet dans le bucket **public** ; l'URL publique (CDN) est calculée par l'adaptateur de stockage.
 */
data class RoomImageVariant(
    val name: String,
    val objectKey: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val contentType: String,
)

/**
 * Image d'une salle. [objectKey] est désormais la clé de l'**original privé** (`rooms/{roomId}/originals/...`) ;
 * les variantes publiques affichables vivent dans [variants] une fois [status] == READY. L'identité [id]
 * (= `image_id`) est reprise dans le préfixe de clé des variantes et sert de corrélation avec le worker.
 */
data class RoomImage(
    val id: RoomImageId,
    val objectKey: String,
    val position: Int,
    val status: RoomImageStatus = RoomImageStatus.PROCESSING,
    val errorCode: String? = null,
    val variants: List<RoomImageVariant> = emptyList(),
)
