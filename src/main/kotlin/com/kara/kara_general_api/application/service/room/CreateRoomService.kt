package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.port.input.room.CreateRoomCommand
import com.kara.kara_general_api.domain.port.input.room.CreateRoomResult
import com.kara.kara_general_api.domain.port.input.room.CreateRoomUseCase
import com.kara.kara_general_api.domain.port.output.GeocodingPort
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service

@Service
class CreateRoomService(
    private val roomRepository: RoomRepository,
    private val geocodingPort: GeocodingPort,
) : CreateRoomUseCase {

    // Le géocodage (appel HTTP externe) est effectué hors transaction :
    // l'unique écriture est le save final, atomique par lui-même.
    override fun createRoom(command: CreateRoomCommand): CreateRoomResult {
        val coordinates = geocodingPort.geocode(command.address) ?: return CreateRoomResult.AddressNotFound
        val room =
            Room.create(
                name = command.name,
                description = command.description,
                address = command.address,
                pricePerPersonPerHour = command.pricePerPersonPerHour,
                currency = command.currency,
                maxCapacity = command.maxCapacity,
                isThereWifi = command.isThereWifi,
                isThereSonoPro = command.isThereSonoPro,
                isThereAirConditioning = command.isThereAirConditioning,
                coordinates = coordinates,
            )
        return CreateRoomResult.Success(roomRepository.save(room))
    }
}
