package com.kara.kara_general_api.domain.port.input.favorite

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.room.RoomPage

data class ListFavoritesQuery(
    val userId: UserId,
    val page: Int = 0,
    val size: Int = 20,
)

interface ListFavoritesUseCase {
    fun listFavorites(query: ListFavoritesQuery): RoomPage
}
