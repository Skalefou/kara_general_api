package com.kara.kara_general_api.infrastructure.adapter.input.rest.room

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.room.CreateRoomCommand
import com.kara.kara_general_api.domain.port.input.room.CreateRoomUseCase
import com.kara.kara_general_api.domain.port.input.room.DeleteRoomResult
import com.kara.kara_general_api.domain.port.input.room.DeleteRoomUseCase
import com.kara.kara_general_api.domain.port.input.room.GetRoomResult
import com.kara.kara_general_api.domain.port.input.room.GetRoomUseCase
import com.kara.kara_general_api.domain.port.input.room.ListRoomsQuery
import com.kara.kara_general_api.domain.port.input.room.ListRoomsUseCase
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomCommand
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomResult
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.CreateRoomRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.RoomListResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.RoomResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.UpdateRoomRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/rooms")
class RoomController(
    private val createRoomUseCase: CreateRoomUseCase,
    private val getRoomUseCase: GetRoomUseCase,
    private val listRoomsUseCase: ListRoomsUseCase,
    private val updateRoomUseCase: UpdateRoomUseCase,
    private val deleteRoomUseCase: DeleteRoomUseCase,
) : RoomApi {

    override fun createRoom(request: CreateRoomRequest): ResponseEntity<RoomResponse> {
        val command = CreateRoomCommand(name = request.name, address = request.toAddress())
        val room = createRoomUseCase.createRoom(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(RoomResponse.from(room))
    }

    override fun listRooms(page: Int, size: Int): ResponseEntity<RoomListResponse> {
        val roomPage = listRoomsUseCase.listRooms(ListRoomsQuery(page = page, size = size))
        return ResponseEntity.ok(RoomListResponse.from(roomPage))
    }

    override fun getRoom(id: UUID): ResponseEntity<Any> =
        when (val result = getRoomUseCase.getRoom(RoomId(id))) {
            is GetRoomResult.Success -> ResponseEntity.ok(RoomResponse.from(result.room))
            GetRoomResult.NotFound -> roomNotFound()
        }

    override fun updateRoom(id: UUID, request: UpdateRoomRequest): ResponseEntity<Any> {
        val command =
            UpdateRoomCommand(
                id = RoomId(id),
                name = request.name,
                street = request.street,
                city = request.city,
                postalCode = request.postalCode,
                country = request.country,
                status = request.status,
            )
        return when (val result = updateRoomUseCase.updateRoom(command)) {
            is UpdateRoomResult.Success -> ResponseEntity.ok(RoomResponse.from(result.room))
            UpdateRoomResult.NotFound -> roomNotFound()
        }
    }

    override fun deleteRoom(id: UUID): ResponseEntity<Any> =
        when (deleteRoomUseCase.deleteRoom(RoomId(id))) {
            DeleteRoomResult.Success -> ResponseEntity.noContent().build()
            DeleteRoomResult.NotFound -> roomNotFound()
        }

    private fun CreateRoomRequest.toAddress(): Address =
        Address(street = street, city = city, postalCode = postalCode, country = country)

    private fun roomNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Aucune salle ne correspond à cet identifiant.",
            ).apply {
                title = "Salle introuvable"
                setProperty("code", "ROOM_NOT_FOUND")
            },
        )
}
