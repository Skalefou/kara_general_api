package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.port.input.room.CreateRoomCommand
import com.kara.kara_general_api.domain.port.input.room.CreateRoomUseCase
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateRoomService(
    private val roomRepository: RoomRepository,
) : CreateRoomUseCase {

    @Transactional
    override fun createRoom(command: CreateRoomCommand): Room {
        val room = Room.create(name = command.name, address = command.address)
        return roomRepository.save(room)
    }
}
