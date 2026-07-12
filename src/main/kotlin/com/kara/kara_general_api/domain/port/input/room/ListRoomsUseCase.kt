package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomCluster
import com.kara.kara_general_api.domain.model.room.vo.BoundingBox

data class ListRoomsQuery(
    val page: Int = 0,
    val size: Int = 20,
    val bbox: BoundingBox? = null,
)

/** Représentation d'un viewport bbox : salles unitaires ou agrégats. */
enum class ViewportMode { ROOMS, CLUSTERS }

data class RoomPage(
    val rooms: List<Room>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    /** Nombre réel de salles dans la bbox. Non nul uniquement en mode bbox. */
    val totalInBbox: Long? = null,
    /** Vrai si des salles ont été écartées par le plafond serveur. Non nul uniquement en mode bbox. */
    val truncated: Boolean? = null,
    /** Mode de rendu du viewport. Non nul uniquement en mode bbox. */
    val mode: ViewportMode? = null,
    /** Agrégats par cellule. Rempli uniquement en mode bbox CLUSTERS, sinon vide. Nul hors mode bbox. */
    val clusters: List<RoomCluster>? = null,
)

interface ListRoomsUseCase {
    fun listRooms(query: ListRoomsQuery): RoomPage
}
