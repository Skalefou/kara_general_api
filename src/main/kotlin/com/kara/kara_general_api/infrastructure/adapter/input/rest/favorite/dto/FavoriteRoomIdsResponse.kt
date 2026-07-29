package com.kara.kara_general_api.infrastructure.adapter.input.rest.favorite.dto

import com.kara.kara_general_api.domain.model.room.RoomId
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class FavoriteRoomIdsResponse(
    @field:Schema(description = "Identifiants de toutes les salles favorites, du favori le plus récent au plus ancien")
    val roomIds: List<UUID>,
) {
    companion object {
        fun from(roomIds: List<RoomId>): FavoriteRoomIdsResponse = FavoriteRoomIdsResponse(roomIds.map { it.value })
    }
}
