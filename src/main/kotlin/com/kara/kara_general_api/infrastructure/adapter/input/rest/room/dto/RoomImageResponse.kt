package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import com.kara.kara_general_api.domain.model.room.RoomImage
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class RoomImageResponse(
    @field:Schema(description = "Identifiant de l'image")
    val id: UUID,
    @field:Schema(description = "URL publique (CDN) de l'image")
    val url: String,
) {
    companion object {
        fun from(image: RoomImage, publicUrl: (String) -> String): RoomImageResponse =
            RoomImageResponse(id = image.id.value, url = publicUrl(image.objectKey))
    }
}
