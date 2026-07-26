package com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.servershift.ServerShiftId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.servershift.CreateServerShiftCommand
import com.kara.kara_general_api.domain.port.input.servershift.CreateServerShiftResult
import com.kara.kara_general_api.domain.port.input.servershift.CreateServerShiftUseCase
import com.kara.kara_general_api.domain.port.input.servershift.DeleteServerShiftResult
import com.kara.kara_general_api.domain.port.input.servershift.DeleteServerShiftUseCase
import com.kara.kara_general_api.domain.port.input.servershift.ListMyShiftsUseCase
import com.kara.kara_general_api.domain.port.input.servershift.ListServerShiftsQuery
import com.kara.kara_general_api.domain.port.input.servershift.ListServerShiftsUseCase
import com.kara.kara_general_api.domain.port.input.servershift.UpdateServerShiftCommand
import com.kara.kara_general_api.domain.port.input.servershift.UpdateServerShiftResult
import com.kara.kara_general_api.domain.port.input.servershift.UpdateServerShiftUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift.dto.CreateServerShiftRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift.dto.ServerShiftResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift.dto.ServerShiftWithRoomResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.servershift.dto.UpdateServerShiftRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/server-shifts")
class ServerShiftController(
    private val listServerShiftsUseCase: ListServerShiftsUseCase,
    private val listMyShiftsUseCase: ListMyShiftsUseCase,
    private val createServerShiftUseCase: CreateServerShiftUseCase,
    private val updateServerShiftUseCase: UpdateServerShiftUseCase,
    private val deleteServerShiftUseCase: DeleteServerShiftUseCase,
) : ServerShiftApi {
    override fun listMyShifts(authentication: Authentication): ResponseEntity<Any> {
        val serverId = UserId(UUID.fromString(authentication.name))
        val shifts = listMyShiftsUseCase.listMyShifts(serverId)
        return ResponseEntity.ok(shifts.map { ServerShiftWithRoomResponse.from(it) })
    }

    override fun listServerShifts(
        serverId: UUID?,
        roomId: UUID?,
        from: Instant?,
        to: Instant?,
    ): ResponseEntity<Any> {
        val shifts =
            listServerShiftsUseCase.listServerShifts(
                ListServerShiftsQuery(
                    serverId = serverId?.let { UserId(it) },
                    roomId = roomId?.let { RoomId(it) },
                    from = from,
                    to = to,
                ),
            )
        return ResponseEntity.ok(shifts.map { ServerShiftResponse.from(it) })
    }

    override fun createServerShift(request: CreateServerShiftRequest): ResponseEntity<Any> {
        val command =
            CreateServerShiftCommand(
                serverId = UserId(request.serverId),
                roomId = RoomId(request.roomId),
                startAt = request.startAt,
                endAt = request.endAt,
                note = request.note,
            )
        return when (val result = createServerShiftUseCase.createServerShift(command)) {
            is CreateServerShiftResult.Created ->
                ResponseEntity.status(HttpStatus.CREATED).body(ServerShiftResponse.from(result.shift))
            CreateServerShiftResult.ServerNotFound -> serverNotFound()
            CreateServerShiftResult.NotAServer -> notAServer()
            CreateServerShiftResult.RoomNotFound -> roomNotFound()
            CreateServerShiftResult.InvalidTimeSlot -> invalidTimeSlot()
            CreateServerShiftResult.SlotUnavailable -> slotUnavailable()
        }
    }

    override fun updateServerShift(
        id: UUID,
        request: UpdateServerShiftRequest,
    ): ResponseEntity<Any> {
        val command =
            UpdateServerShiftCommand(
                id = ServerShiftId(id),
                roomId = request.roomId?.let { RoomId(it) },
                startAt = request.startAt,
                endAt = request.endAt,
                note = request.note,
                clearNote = request.clearNote,
            )
        return when (val result = updateServerShiftUseCase.updateServerShift(command)) {
            is UpdateServerShiftResult.Success -> ResponseEntity.ok(ServerShiftResponse.from(result.shift))
            UpdateServerShiftResult.NotFound -> shiftNotFound()
            UpdateServerShiftResult.RoomNotFound -> roomNotFound()
            UpdateServerShiftResult.InvalidTimeSlot -> invalidTimeSlot()
            UpdateServerShiftResult.SlotUnavailable -> slotUnavailable()
        }
    }

    override fun deleteServerShift(id: UUID): ResponseEntity<Any> =
        when (deleteServerShiftUseCase.deleteServerShift(ServerShiftId(id))) {
            DeleteServerShiftResult.Success -> ResponseEntity.noContent().build()
            DeleteServerShiftResult.NotFound -> shiftNotFound()
        }

    private fun serverNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Aucun serveur ne correspond à cet identifiant.",
                ).apply {
                    title = "Serveur introuvable"
                    setProperty("code", "SERVER_NOT_FOUND")
                },
        )

    private fun notAServer(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "Ce compte n'est pas un compte serveur.",
                ).apply {
                    title = "Compte non serveur"
                    setProperty("code", "NOT_A_SERVER")
                },
        )

    private fun roomNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Aucune salle ne correspond à cet identifiant.",
                ).apply {
                    title = "Salle introuvable"
                    setProperty("code", "ROOM_NOT_FOUND")
                },
        )

    private fun invalidTimeSlot(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.BAD_REQUEST,
                    "L'heure de fin doit être strictement postérieure à l'heure de début.",
                ).apply {
                    title = "Créneau invalide"
                    setProperty("code", "INVALID_TIME_SLOT")
                },
        )

    private fun slotUnavailable(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "Ce créneau chevauche un autre créneau de ce serveur.",
                ).apply {
                    title = "Créneau indisponible"
                    setProperty("code", "SHIFT_SLOT_UNAVAILABLE")
                },
        )

    private fun shiftNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail
                .forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    "Aucun créneau ne correspond à cet identifiant.",
                ).apply {
                    title = "Créneau introuvable"
                    setProperty("code", "SHIFT_NOT_FOUND")
                },
        )
}
