package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.vo.Address
import java.math.BigDecimal

data class CreateRoomCommand(
    val name: String,
    val description: String,
    val address: Address,
    val pricePerPersonPerHour: BigDecimal,
    val currency: Currency,
    val isThereWifi: Boolean,
    val isThereSonoPro: Boolean,
    val isThereAirConditioning: Boolean,
)

sealed interface CreateRoomResult {
    data class Success(val room: Room) : CreateRoomResult

    data object AddressNotFound : CreateRoomResult
}

interface CreateRoomUseCase {
    fun createRoom(command: CreateRoomCommand): CreateRoomResult
}
