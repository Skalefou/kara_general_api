package com.kara.kara_general_api.domain.port.input.favorite

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId

data class RemoveFavoriteCommand(
    val userId: UserId,
    val roomId: RoomId,
)

sealed interface RemoveFavoriteResult {
    data object Success : RemoveFavoriteResult

    data object NotFound : RemoveFavoriteResult
}

interface RemoveFavoriteUseCase {
    fun removeFavorite(command: RemoveFavoriteCommand): RemoveFavoriteResult
}
