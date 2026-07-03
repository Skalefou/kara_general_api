package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.port.input.room.UpdateRoomCommand
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomResult
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomUseCase
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateRoomService(
    private val roomRepository: RoomRepository,
) : UpdateRoomUseCase {

    @Transactional
    override fun updateRoom(command: UpdateRoomCommand): UpdateRoomResult {
        val existing = roomRepository.findById(command.id) ?: return UpdateRoomResult.NotFound
        val updated = existing.update(name = command.name, address = command.address)
        return UpdateRoomResult.Success(roomRepository.save(updated))
    }
}
