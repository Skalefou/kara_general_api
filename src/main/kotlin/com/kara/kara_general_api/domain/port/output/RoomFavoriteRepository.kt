package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId

interface RoomFavoriteRepository {
    /** Ajoute la salle aux favoris de l'utilisateur. Retourne false si le favori existait déjà. */
    fun add(
        userId: UserId,
        roomId: RoomId,
    ): Boolean

    /** Retire la salle des favoris. Retourne false si aucun favori ne correspondait. */
    fun remove(
        userId: UserId,
        roomId: RoomId,
    ): Boolean

    /** Identifiants des salles favorites, page par page, du favori le plus récent au plus ancien. */
    fun findRoomIdsByUser(
        userId: UserId,
        page: Int,
        size: Int,
    ): List<RoomId>

    /** Ensemble complet des identifiants favoris de l'utilisateur, sans pagination. */
    fun findAllRoomIdsByUser(userId: UserId): List<RoomId>

    fun countByUser(userId: UserId): Long
}
