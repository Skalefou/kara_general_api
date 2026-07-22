package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.CreateBookingCommand
import com.kara.kara_general_api.domain.port.input.booking.CreateBookingResult
import com.kara.kara_general_api.domain.port.input.booking.CreateBookingUseCase
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingCommand
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingResult
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingUseCase
import com.kara.kara_general_api.domain.port.input.booking.ListAllBookingsUseCase
import com.kara.kara_general_api.domain.port.input.booking.ListServerBookingsUseCase
import com.kara.kara_general_api.domain.port.input.booking.TriggerEmergencyCommand
import com.kara.kara_general_api.domain.port.input.booking.TriggerEmergencyResult
import com.kara.kara_general_api.domain.port.input.booking.TriggerEmergencyUseCase
import com.kara.kara_general_api.domain.port.input.chat.OpenBookingConversationCommand
import com.kara.kara_general_api.domain.port.input.chat.OpenBookingConversationResult
import com.kara.kara_general_api.domain.port.input.chat.OpenBookingConversationUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.AdminBookingResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.BookingConversationResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.BookingResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.CreateBookingRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.EstimateBookingRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.EstimateBookingResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.ServerBookingResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/bookings")
class BookingController(
    private val estimateBookingUseCase: EstimateBookingUseCase,
    private val createBookingUseCase: CreateBookingUseCase,
    private val listServerBookingsUseCase: ListServerBookingsUseCase,
    private val listAllBookingsUseCase: ListAllBookingsUseCase,
    private val openBookingConversationUseCase: OpenBookingConversationUseCase,
    private val triggerEmergencyUseCase: TriggerEmergencyUseCase,
) : BookingApi {

    override fun triggerEmergency(id: UUID, authentication: Authentication): ResponseEntity<Any> {
        val command =
            TriggerEmergencyCommand(
                bookingId = BookingId(id),
                currentUserId = UserId(UUID.fromString(authentication.name)),
                isAdmin = authentication.authorities.any { it.authority == "ROLE_ADMIN" },
            )
        return when (triggerEmergencyUseCase.triggerEmergency(command)) {
            is TriggerEmergencyResult.Success -> ResponseEntity.noContent().build()
            TriggerEmergencyResult.BookingNotFound -> bookingNotFound()
            TriggerEmergencyResult.NotAuthorized -> notAuthorized()
        }
    }

    override fun listMyAssignedBookings(authentication: Authentication): ResponseEntity<Any> {
        val serverId = UserId(UUID.fromString(authentication.name))
        val bookings = listServerBookingsUseCase.listServerBookings(serverId)
        return ResponseEntity.ok(bookings.map { ServerBookingResponse.from(it) })
    }

    override fun listAllBookings(): ResponseEntity<Any> =
        ResponseEntity.ok(listAllBookingsUseCase.listAllBookings().map { AdminBookingResponse.from(it) })

    override fun openBookingConversation(id: UUID, authentication: Authentication): ResponseEntity<Any> {
        val command =
            OpenBookingConversationCommand(
                bookingId = BookingId(id),
                currentUserId = UserId(UUID.fromString(authentication.name)),
                isAdmin = authentication.authorities.any { it.authority == "ROLE_ADMIN" },
            )
        return when (val result = openBookingConversationUseCase.openBookingConversation(command)) {
            is OpenBookingConversationResult.Success ->
                ResponseEntity.ok(BookingConversationResponse.from(result))
            OpenBookingConversationResult.BookingNotFound -> bookingNotFound()
            OpenBookingConversationResult.NotAuthorized -> notAuthorized()
        }
    }

    private fun bookingNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Aucune réservation ne correspond à cet identifiant.",
            ).apply {
                title = "Réservation introuvable"
                setProperty("code", "BOOKING_NOT_FOUND")
            },
        )

    private fun notAuthorized(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Vous n'êtes pas autorisé à accéder au chat de cette réservation.",
            ).apply {
                title = "Accès refusé"
                setProperty("code", "NOT_AUTHORIZED")
            },
        )

    override fun createBooking(
        request: CreateBookingRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            CreateBookingCommand(
                roomId = RoomId(request.roomId),
                userId = UserId(UUID.fromString(authentication.name)),
                startAt = request.startAt,
                endAt = request.endAt,
                numberOfPeople = request.numberOfPeople,
                selectedOptionIds = request.optionIds.map { RoomOptionId(it) },
            )
        return when (val result = createBookingUseCase.createBooking(command)) {
            is CreateBookingResult.Created ->
                ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(result.booking))
            CreateBookingResult.RoomNotFound -> roomNotFound()
            CreateBookingResult.TooFewPeople -> tooFewPeople()
            is CreateBookingResult.CapacityExceeded -> capacityExceeded(result.maxCapacity)
            CreateBookingResult.InvalidTimeSlot -> invalidTimeSlot()
            is CreateBookingResult.UnknownOptions -> unknownOptions(result.optionIds)
            CreateBookingResult.SlotUnavailable -> slotUnavailable()
        }
    }

    override fun estimate(request: EstimateBookingRequest): ResponseEntity<Any> {
        val command =
            EstimateBookingCommand(
                roomId = RoomId(request.roomId),
                startAt = request.startAt,
                endAt = request.endAt,
                numberOfPeople = request.numberOfPeople,
                optionIds = request.optionIds.map { RoomOptionId(it) },
            )
        return when (val result = estimateBookingUseCase.estimate(command)) {
            is EstimateBookingResult.Success ->
                ResponseEntity.ok(EstimateBookingResponse.from(result.estimate))
            EstimateBookingResult.RoomNotFound -> roomNotFound()
            EstimateBookingResult.TooFewPeople -> tooFewPeople()
            is EstimateBookingResult.CapacityExceeded -> capacityExceeded(result.maxCapacity)
            EstimateBookingResult.InvalidTimeSlot -> invalidTimeSlot()
            is EstimateBookingResult.UnknownOptions -> unknownOptions(result.optionIds)
        }
    }

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

    private fun tooFewPeople(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Une réservation nécessite au minimum 2 personnes.",
            ).apply {
                title = "Nombre de personnes insuffisant"
                setProperty("code", "TOO_FEW_PEOPLE")
            },
        )

    private fun capacityExceeded(maxCapacity: Int): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Le nombre de personnes dépasse la capacité maximale de la salle ($maxCapacity).",
            ).apply {
                title = "Capacité dépassée"
                setProperty("code", "CAPACITY_EXCEEDED")
            },
        )

    private fun invalidTimeSlot(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "L'heure de fin doit être strictement postérieure à l'heure de début.",
            ).apply {
                title = "Créneau invalide"
                setProperty("code", "INVALID_TIME_SLOT")
            },
        )

    private fun slotUnavailable(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Ce créneau chevauche une réservation existante pour cette salle.",
            ).apply {
                title = "Créneau indisponible"
                setProperty("code", "BOOKING_SLOT_UNAVAILABLE")
            },
        )

    private fun unknownOptions(optionIds: List<UUID>): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Certaines options ne sont pas proposées par cette salle : ${optionIds.joinToString(", ")}.",
            ).apply {
                title = "Option invalide"
                setProperty("code", "UNKNOWN_ROOM_OPTION")
                setProperty("optionIds", optionIds.map { it.toString() })
            },
        )
}
