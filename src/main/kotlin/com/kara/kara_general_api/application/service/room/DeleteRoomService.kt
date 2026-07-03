package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.port.input.room.DeleteRoomResult
import com.kara.kara_general_api.domain.port.input.room.DeleteRoomUseCase
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteRoomService(
    private val roomRepository: RoomRepository,
) : DeleteRoomUseCase {

    @Transactional
    override fun deleteRoom(id: RoomId): DeleteRoomResult {
        val deleted = roomRepository.deleteById(id)
        return if (deleted) DeleteRoomResult.Success else DeleteRoomResult.NotFound
    }
}
