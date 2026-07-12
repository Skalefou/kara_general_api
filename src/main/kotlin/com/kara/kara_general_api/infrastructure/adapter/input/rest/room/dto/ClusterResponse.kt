package com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto

import com.kara.kara_general_api.domain.model.room.RoomCluster
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Agrégat de salles pour une cellule de la grille de clustering")
data class ClusterResponse(
    @field:Schema(description = "Latitude du centroïde des salles agrégées", example = "48.86")
    val latitude: Double,
    @field:Schema(description = "Longitude du centroïde des salles agrégées", example = "2.34")
    val longitude: Double,
    @field:Schema(description = "Nombre de salles dans la cellule", example = "42")
    val count: Long,
) {
    companion object {
        fun from(cluster: RoomCluster): ClusterResponse =
            ClusterResponse(
                latitude = cluster.latitude,
                longitude = cluster.longitude,
                count = cluster.count,
            )
    }
}
