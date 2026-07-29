package com.kara.kara_general_api.domain.port.input.favorite

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId

interface ListFavoriteRoomIdsUseCase {
    fun listFavoriteRoomIds(userId: UserId): List<RoomId>
}
