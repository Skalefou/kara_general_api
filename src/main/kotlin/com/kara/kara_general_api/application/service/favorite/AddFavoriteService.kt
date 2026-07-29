package com.kara.kara_general_api.application.service.favorite

import com.kara.kara_general_api.domain.port.input.favorite.AddFavoriteCommand
import com.kara.kara_general_api.domain.port.input.favorite.AddFavoriteResult
import com.kara.kara_general_api.domain.port.input.favorite.AddFavoriteUseCase
import com.kara.kara_general_api.domain.port.output.RoomFavoriteRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AddFavoriteService(
    private val roomRepository: RoomRepository,
    private val roomFavoriteRepository: RoomFavoriteRepository,
) : AddFavoriteUseCase {
    @Transactional
    override fun addFavorite(command: AddFavoriteCommand): AddFavoriteResult {
        roomRepository.findById(command.roomId) ?: return AddFavoriteResult.RoomNotFound
        roomFavoriteRepository.add(command.userId, command.roomId)
        return AddFavoriteResult.Success
    }
}
