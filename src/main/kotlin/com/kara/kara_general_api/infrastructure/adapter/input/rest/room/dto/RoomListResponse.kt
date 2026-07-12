package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import com.kara.kara_general_api.domain.port.input.room.RoomPage
import com.kara.kara_general_api.domain.port.input.room.ViewportMode
import io.swagger.v3.oas.annotations.media.Schema

data class RoomListResponse(
    @field:Schema(
        description = "Mode de rendu du viewport : \"rooms\" (salles unitaires) ou \"clusters\" (agrégats). " +
            "Présent uniquement en mode filtrage bbox.",
        nullable = true,
        allowableValues = ["rooms", "clusters"],
    )
    val mode: String? = null,
    @field:Schema(description = "Salles de la page courante ; vide en mode clusters")
    val rooms: List<RoomResponse>,
    @field:Schema(
        description = "Agrégats par cellule ; rempli uniquement en mode clusters, sinon vide. " +
            "Présent uniquement en mode filtrage bbox.",
        nullable = true,
    )
    val clusters: List<ClusterResponse>? = null,
    @field:Schema(description = "Numéro de la page courante (0-indexed)")
    val page: Int,
    @field:Schema(description = "Nombre d'éléments par page")
    val size: Int,
    @field:Schema(description = "Nombre total de salles")
    val totalElements: Long,
    @field:Schema(description = "Nombre total de pages")
    val totalPages: Int,
    @field:Schema(
        description = "Nombre réel de salles dans la bbox avant plafonnement serveur. " +
            "Présent uniquement en mode filtrage bbox.",
        nullable = true,
    )
    val totalInBbox: Long? = null,
    @field:Schema(
        description = "Vrai si des salles dans la bbox ont été écartées par le plafond serveur. " +
            "Présent uniquement en mode filtrage bbox.",
        nullable = true,
    )
    val truncated: Boolean? = null,
) {
    companion object {
        fun from(roomPage: RoomPage, publicUrl: (String) -> String): RoomListResponse =
            RoomListResponse(
                mode =
                    when (roomPage.mode) {
                        ViewportMode.ROOMS -> "rooms"
                        ViewportMode.CLUSTERS -> "clusters"
                        null -> null
                    },
                rooms = roomPage.rooms.map { RoomResponse.from(it, publicUrl) },
                clusters = roomPage.clusters?.map { ClusterResponse.from(it) },
                page = roomPage.page,
                size = roomPage.size,
                totalElements = roomPage.totalElements,
                totalPages =
                    if (roomPage.size == 0) {
                        0
                    } else {
                        ((roomPage.totalElements + roomPage.size - 1) / roomPage.size).toInt()
                    },
                totalInBbox = roomPage.totalInBbox,
                truncated = roomPage.truncated,
            )
    }
}
