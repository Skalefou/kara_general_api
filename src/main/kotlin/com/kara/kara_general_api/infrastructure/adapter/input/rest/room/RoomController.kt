package com.kara.kara_general_api.infrastructure.adapter.input.rest.room

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomImageId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageCommand
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageResult
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageUseCase
import com.kara.kara_general_api.domain.port.input.room.CreateRoomCommand
import com.kara.kara_general_api.domain.port.input.room.CreateRoomUseCase
import com.kara.kara_general_api.domain.port.input.room.DeleteRoomResult
import com.kara.kara_general_api.domain.port.input.room.DeleteRoomUseCase
import com.kara.kara_general_api.domain.port.input.room.GetRoomResult
import com.kara.kara_general_api.domain.port.input.room.GetRoomUseCase
import com.kara.kara_general_api.domain.port.input.room.ListRoomsQuery
import com.kara.kara_general_api.domain.port.input.room.ListRoomsUseCase
import com.kara.kara_general_api.domain.port.input.room.RemoveRoomImageCommand
import com.kara.kara_general_api.domain.port.input.room.RemoveRoomImageResult
import com.kara.kara_general_api.domain.port.input.room.RemoveRoomImageUseCase
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomCommand
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomResult
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.CreateRoomRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.RoomImageResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.RoomListResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.RoomResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.room.dto.UpdateRoomRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/v1/rooms")
class RoomController(
    private val createRoomUseCase: CreateRoomUseCase,
    private val getRoomUseCase: GetRoomUseCase,
    private val listRoomsUseCase: ListRoomsUseCase,
    private val updateRoomUseCase: UpdateRoomUseCase,
    private val deleteRoomUseCase: DeleteRoomUseCase,
    private val addRoomImageUseCase: AddRoomImageUseCase,
    private val removeRoomImageUseCase: RemoveRoomImageUseCase,
    private val imageStorage: ImageStoragePort,
) : RoomApi {

    override fun createRoom(request: CreateRoomRequest): ResponseEntity<RoomResponse> {
        val command = CreateRoomCommand(name = request.name, address = request.toAddress())
        val room = createRoomUseCase.createRoom(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(RoomResponse.from(room, imageStorage::publicUrl))
    }

    override fun listRooms(page: Int, size: Int): ResponseEntity<RoomListResponse> {
        val roomPage = listRoomsUseCase.listRooms(ListRoomsQuery(page = page, size = size))
        return ResponseEntity.ok(RoomListResponse.from(roomPage, imageStorage::publicUrl))
    }

    override fun getRoom(id: UUID): ResponseEntity<Any> =
        when (val result = getRoomUseCase.getRoom(RoomId(id))) {
            is GetRoomResult.Success -> ResponseEntity.ok(RoomResponse.from(result.room, imageStorage::publicUrl))
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
            is UpdateRoomResult.Success -> ResponseEntity.ok(RoomResponse.from(result.room, imageStorage::publicUrl))
            UpdateRoomResult.NotFound -> roomNotFound()
        }
    }

    override fun deleteRoom(id: UUID): ResponseEntity<Any> =
        when (deleteRoomUseCase.deleteRoom(RoomId(id))) {
            DeleteRoomResult.Success -> ResponseEntity.noContent().build()
            DeleteRoomResult.NotFound -> roomNotFound()
        }

    override fun addRoomImage(id: UUID, file: MultipartFile): ResponseEntity<Any> {
        val command =
            AddRoomImageCommand(
                roomId = RoomId(id),
                bytes = file.bytes,
                contentType = file.contentType,
            )

        return when (val result = addRoomImageUseCase.addImage(command)) {
            is AddRoomImageResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(
                    RoomImageResponse(id = result.image.id.value, url = result.url),
                )

            AddRoomImageResult.RoomNotFound -> roomNotFound()
            AddRoomImageResult.InvalidImageType -> invalidImageType()
            AddRoomImageResult.ImageTooLarge -> imageTooLarge()
        }
    }

    override fun removeRoomImage(id: UUID, imageId: UUID): ResponseEntity<Any> =
        when (removeRoomImageUseCase.removeImage(RemoveRoomImageCommand(RoomId(id), RoomImageId(imageId)))) {
            RemoveRoomImageResult.Success -> ResponseEntity.noContent().build()
            RemoveRoomImageResult.RoomNotFound -> roomNotFound()
            RemoveRoomImageResult.ImageNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        "Aucune image ne correspond à cet identifiant pour cette salle.",
                    ).apply {
                        title = "Image introuvable"
                        setProperty("code", "ROOM_IMAGE_NOT_FOUND")
                    },
                )
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

    private fun invalidImageType(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Format d'image non supporté. Formats acceptés : JPEG, PNG, WebP, AVIF, HEIC.",
            ).apply {
                title = "Format d'image non supporté"
                setProperty("code", "INVALID_IMAGE_TYPE")
            },
        )

    private fun imageTooLarge(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "L'image dépasse la taille maximale autorisée (5 Mo).",
            ).apply {
                title = "Image trop volumineuse"
                setProperty("code", "IMAGE_TOO_LARGE")
            },
        )
}
