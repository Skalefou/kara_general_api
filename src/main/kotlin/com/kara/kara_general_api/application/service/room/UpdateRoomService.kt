package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.room.vo.Coordinates
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomCommand
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomResult
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomUseCase
import com.kara.kara_general_api.domain.port.output.GeocodingPort
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service

@Service
class UpdateRoomService(
    private val roomRepository: RoomRepository,
    private val geocodingPort: GeocodingPort,
) : UpdateRoomUseCase {

    // Le géocodage (appel HTTP externe) n'est déclenché que si l'adresse change,
    // et reste hors transaction : l'unique écriture est le save final.
    override fun updateRoom(command: UpdateRoomCommand): UpdateRoomResult {
        val existing = roomRepository.findById(command.id) ?: return UpdateRoomResult.NotFound
        val mergedAddress =
            Address(
                street = command.street ?: existing.address.street,
                city = command.city ?: existing.address.city,
                postalCode = command.postalCode ?: existing.address.postalCode,
                country = command.country ?: existing.address.country,
            )
        val coordinates =
            if (mergedAddress == existing.address) {
                // Adresse inchangée : on conserve les coordonnées existantes (éventuellement absentes).
                existing.latitude?.let { lat -> existing.longitude?.let { lon -> Coordinates(lat, lon) } }
            } else {
                geocodingPort.geocode(mergedAddress) ?: return UpdateRoomResult.AddressNotFound
            }
        val updated =
            existing.update(
                name = command.name ?: existing.name,
                description = command.description ?: existing.description,
                address = mergedAddress,
                pricePerPersonPerHour = command.pricePerPersonPerHour ?: existing.pricePerPersonPerHour,
                currency = command.currency ?: existing.currency,
                isThereWifi = command.isThereWifi ?: existing.isThereWifi,
                isThereSonoPro = command.isThereSonoPro ?: existing.isThereSonoPro,
                isThereAirConditioning = command.isThereAirConditioning ?: existing.isThereAirConditioning,
                status = command.status ?: existing.status,
                coordinates = coordinates,
            )
        return UpdateRoomResult.Success(roomRepository.save(updated))
    }
}
