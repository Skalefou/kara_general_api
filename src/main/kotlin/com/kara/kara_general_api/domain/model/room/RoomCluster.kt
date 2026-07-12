package com.kara.kara_general_api.domain.model.room

/**
 * Agrégat de salles pour une cellule de la grille de clustering d'un viewport.
 *
 * [latitude]/[longitude] = centroïde des salles de la cellule ; [count] = nombre de salles agrégées.
 */
data class RoomCluster(
    val latitude: Double,
    val longitude: Double,
    val count: Long,
)
