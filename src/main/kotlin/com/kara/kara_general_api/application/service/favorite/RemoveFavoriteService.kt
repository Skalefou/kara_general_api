package com.kara.kara_general_api.application.service.favorite

import com.kara.kara_general_api.domain.port.input.favorite.RemoveFavoriteCommand
import com.kara.kara_general_api.domain.port.input.favorite.RemoveFavoriteResult
import com.kara.kara_general_api.domain.port.input.favorite.RemoveFavoriteUseCase
import com.kara.kara_general_api.domain.port.output.RoomFavoriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RemoveFavoriteService(
    private val roomFavoriteRepository: RoomFavoriteRepository,
) : RemoveFavoriteUseCase {
    @Transactional
    override fun removeFavorite(command: RemoveFavoriteCommand): RemoveFavoriteResult =
        if (roomFavoriteRepository.remove(command.userId, command.roomId)) {
            RemoveFavoriteResult.Success
        } else {
            RemoveFavoriteResult.NotFound
        }
}
