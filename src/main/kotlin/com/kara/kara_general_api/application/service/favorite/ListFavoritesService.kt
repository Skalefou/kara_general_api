package com.kara.kara_general_api.application.service.favorite

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.favorite.ListFavoriteRoomIdsUseCase
import com.kara.kara_general_api.domain.port.input.favorite.ListFavoritesQuery
import com.kara.kara_general_api.domain.port.input.favorite.ListFavoritesUseCase
import com.kara.kara_general_api.domain.port.input.room.RoomPage
import com.kara.kara_general_api.domain.port.output.RoomFavoriteRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service

@Service
class ListFavoritesService(
    private val roomFavoriteRepository: RoomFavoriteRepository,
    private val roomRepository: RoomRepository,
) : ListFavoritesUseCase,
    ListFavoriteRoomIdsUseCase {
    // Deux requêtes seulement : la page d'identifiants favoris (triée par date d'ajout), puis les salles
    // correspondantes chargées en un seul IN. L'ordre « favori le plus récent d'abord » est réappliqué en
    // mémoire, findByIds ne le garantissant pas.
    override fun listFavorites(query: ListFavoritesQuery): RoomPage {
        val roomIds = roomFavoriteRepository.findRoomIdsByUser(query.userId, query.page, query.size)
        val roomsById = roomRepository.findByIds(roomIds).associateBy { it.id }
        return RoomPage(
            rooms = roomIds.mapNotNull { roomsById[it] },
            page = query.page,
            size = query.size,
            totalElements = roomFavoriteRepository.countByUser(query.userId),
        )
    }

    override fun listFavoriteRoomIds(userId: UserId): List<RoomId> = roomFavoriteRepository.findAllRoomIdsByUser(userId)
}
