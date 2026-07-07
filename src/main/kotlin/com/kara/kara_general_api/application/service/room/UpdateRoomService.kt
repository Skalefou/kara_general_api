package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.vo.Address
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
        val mergedAddress =
            Address(
                street = command.street ?: existing.address.street,
                city = command.city ?: existing.address.city,
                postalCode = command.postalCode ?: existing.address.postalCode,
                country = command.country ?: existing.address.country,
            )
        val updated =
            existing.update(
                name = command.name ?: existing.name,
                address = mergedAddress,
                status = command.status ?: existing.status,
            )
        return UpdateRoomResult.Success(roomRepository.save(updated))
    }
}
