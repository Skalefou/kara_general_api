package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import com.kara.kara_general_api.domain.model.room.RoomImage
import com.kara.kara_general_api.domain.model.room.RoomImageStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * Représentation d'une image de salle en lecture. Une image n'expose ses URL de variantes (CDN) que lorsqu'elle
 * est READY ; en PROCESSING ou FAILED, seul le [status] (et [errorCode] en cas d'échec) est renseigné.
 */
data class RoomImageResponse(
    @field:Schema(description = "Identifiant de l'image")
    val id: UUID,
    @field:Schema(description = "Statut de traitement", allowableValues = ["PROCESSING", "READY", "FAILED"])
    val status: String,
    @field:Schema(
        description = "URL publiques (CDN) par variante (thumbnail/detail/full). Présent uniquement si READY.",
        nullable = true,
    )
    val variants: Map<String, String>? = null,
    @field:Schema(description = "Code d'erreur du worker. Présent uniquement si FAILED.", nullable = true)
    val errorCode: String? = null,
) {
    companion object {
        fun from(image: RoomImage, publicUrl: (String) -> String): RoomImageResponse =
            RoomImageResponse(
                id = image.id.value,
                status = image.status.name,
                variants =
                    if (image.status == RoomImageStatus.READY) {
                        image.variants.associate { it.name to publicUrl(it.objectKey) }
                    } else {
                        null
                    },
                errorCode = if (image.status == RoomImageStatus.FAILED) image.errorCode else null,
            )
    }
}
