package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.service.ServiceId
import java.math.BigDecimal

data class CreateRoomCommand(
    val name: String,
    val description: String,
    val address: Address,
    val pricePerPersonPerHour: BigDecimal,
    val currency: Currency,
    val maxCapacity: Int,
    val isThereWifi: Boolean,
    val isThereSonoPro: Boolean,
    val isThereAirConditioning: Boolean,
    val serviceIds: List<ServiceId> = emptyList(),
)

sealed interface CreateRoomResult {
    data class Success(val room: Room) : CreateRoomResult

    data object AddressNotFound : CreateRoomResult

    /** Un des services référencés n'existe pas dans le catalogue global. */
    data class UnknownService(val serviceId: ServiceId) : CreateRoomResult
}

interface CreateRoomUseCase {
    fun createRoom(command: CreateRoomCommand): CreateRoomResult
}
