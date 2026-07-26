package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomStatus
import com.kara.kara_general_api.domain.model.service.ServiceId
import java.math.BigDecimal

data class UpdateRoomCommand(
    val id: RoomId,
    val name: String?,
    val description: String?,
    val street: String?,
    val city: String?,
    val postalCode: String?,
    val country: String?,
    val pricePerPersonPerHour: BigDecimal?,
    val currency: Currency?,
    val maxCapacity: Int?,
    val isThereWifi: Boolean?,
    val isThereSonoPro: Boolean?,
    val isThereAirConditioning: Boolean?,
    val status: RoomStatus?,
    // null = liaisons inchangées ; une liste (même vide) remplace l'ensemble des liaisons.
    val serviceIds: List<ServiceId>? = null,
)

sealed interface UpdateRoomResult {
    data class Success(
        val room: Room,
    ) : UpdateRoomResult

    data object NotFound : UpdateRoomResult

    data object AddressNotFound : UpdateRoomResult

    /** Un des services référencés n'existe pas dans le catalogue global. */
    data class UnknownService(
        val serviceId: ServiceId,
    ) : UpdateRoomResult
}

interface UpdateRoomUseCase {
    fun updateRoom(command: UpdateRoomCommand): UpdateRoomResult
}
