package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import com.kara.kara_general_api.domain.port.input.room.RoomPage
import io.swagger.v3.oas.annotations.media.Schema

data class RoomListResponse(
    @field:Schema(description = "Salles de la page courante")
    val rooms: List<RoomResponse>,
    @field:Schema(description = "Numéro de la page courante (0-indexed)")
    val page: Int,
    @field:Schema(description = "Nombre d'éléments par page")
    val size: Int,
    @field:Schema(description = "Nombre total de salles")
    val totalElements: Long,
    @field:Schema(description = "Nombre total de pages")
    val totalPages: Int,
) {
    companion object {
        fun from(roomPage: RoomPage, publicUrl: (String) -> String): RoomListResponse =
            RoomListResponse(
                rooms = roomPage.rooms.map { RoomResponse.from(it, publicUrl) },
                page = roomPage.page,
                size = roomPage.size,
                totalElements = roomPage.totalElements,
                totalPages =
                    if (roomPage.size == 0) {
                        0
                    } else {
                        ((roomPage.totalElements + roomPage.size - 1) / roomPage.size).toInt()
                    },
            )
    }
}
