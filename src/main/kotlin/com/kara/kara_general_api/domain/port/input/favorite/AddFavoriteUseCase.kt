package com.kara.kara_general_api.domain.port.input.favorite

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId

data class AddFavoriteCommand(
    val userId: UserId,
    val roomId: RoomId,
)

sealed interface AddFavoriteResult {
    /** Favori enregistré, ou déjà présent : l'opération est idempotente. */
    data object Success : AddFavoriteResult

    data object RoomNotFound : AddFavoriteResult
}

interface AddFavoriteUseCase {
    fun addFavorite(command: AddFavoriteCommand): AddFavoriteResult
}
