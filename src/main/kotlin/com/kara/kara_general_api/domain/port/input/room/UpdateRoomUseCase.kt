package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomStatus
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
    val isThereWifi: Boolean?,
    val isThereSonoPro: Boolean?,
    val isThereAirConditioning: Boolean?,
    val status: RoomStatus?,
)

sealed interface UpdateRoomResult {
    data class Success(val room: Room) : UpdateRoomResult

    data object NotFound : UpdateRoomResult

    data object AddressNotFound : UpdateRoomResult
}

interface UpdateRoomUseCase {
    fun updateRoom(command: UpdateRoomCommand): UpdateRoomResult
}
