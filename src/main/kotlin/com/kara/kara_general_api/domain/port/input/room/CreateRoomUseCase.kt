package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.vo.Address
import java.math.BigDecimal

data class CreateRoomCommand(
    val name: String,
    val address: Address,
    val pricePerPersonPerHour: BigDecimal,
)

sealed interface CreateRoomResult {
    data class Success(val room: Room) : CreateRoomResult

    data object AddressNotFound : CreateRoomResult
}

interface CreateRoomUseCase {
    fun createRoom(command: CreateRoomCommand): CreateRoomResult
}
