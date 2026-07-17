package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.port.input.room.CreateRoomCommand
import com.kara.kara_general_api.domain.port.input.room.CreateRoomResult
import com.kara.kara_general_api.domain.port.input.room.CreateRoomUseCase
import com.kara.kara_general_api.domain.port.output.GeocodingPort
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.RoomServiceRepository
import com.kara.kara_general_api.domain.port.output.ServiceRepository
import org.springframework.stereotype.Service

@Service
class CreateRoomService(
    private val roomRepository: RoomRepository,
    private val geocodingPort: GeocodingPort,
    private val serviceRepository: ServiceRepository,
    private val roomServiceRepository: RoomServiceRepository,
) : CreateRoomUseCase {

    // Le géocodage (appel HTTP externe) est effectué hors transaction :
    // les écritures (save de la salle + liaisons de services) suivent la validation des services.
    override fun createRoom(command: CreateRoomCommand): CreateRoomResult {
        // Valide l'existence des services avant toute écriture pour éviter une salle orpheline
        // en cas de service inconnu (et pour ne pas dépendre d'une violation de contrainte FK).
        command.serviceIds.firstOrNull { !serviceRepository.existsById(it) }
            ?.let { return CreateRoomResult.UnknownService(it) }

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
        val saved = roomRepository.save(room)
        roomServiceRepository.addLinks(saved.id, command.serviceIds)
        return CreateRoomResult.Success(saved)
    }
}
