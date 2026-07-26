package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.ValidateBookingAccessCommand
import com.kara.kara_general_api.domain.port.input.booking.ValidateBookingAccessResult
import com.kara.kara_general_api.domain.port.input.booking.ValidateBookingAccessUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.BookingAccessResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/bookings")
class BookingAccessController(
    private val validateBookingAccessUseCase: ValidateBookingAccessUseCase,
) : BookingAccessApi {

    @PostMapping("/{id}/validate-access")
    override fun validateAccess(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            ValidateBookingAccessCommand(
                bookingId = BookingId(id),
                currentUserId = UserId(UUID.fromString(authentication.name)),
                isAdmin = authentication.authorities.any { it.authority == "ROLE_ADMIN" },
            )
        return when (val result = validateBookingAccessUseCase.validate(command)) {
            is ValidateBookingAccessResult.Granted ->
                ResponseEntity.ok(BookingAccessResponse.granted(result.view, result.checkedInAt))

            is ValidateBookingAccessResult.AlreadyCheckedIn ->
                refusal(
                    detail = "Ce billet a déjà été validé.",
                    code = "BOOKING_ALREADY_CHECKED_IN",
                    title = "Billet déjà validé",
                    booking =
                        BookingAccessResponse.alreadyCheckedIn(
                            result.view,
                            result.firstCheckedInAt,
                            result.checkedInByName,
                        ),
                )

            is ValidateBookingAccessResult.NotConfirmed ->
                refusal(
                    detail = "La réservation n'est pas confirmée : le billet ne donne pas accès.",
                    code = "BOOKING_NOT_CONFIRMED",
                    title = "Réservation non confirmée",
                    booking = BookingAccessResponse.from(result.view),
                )

            is ValidateBookingAccessResult.OutsideAdmissionWindow ->
                refusal(
                    detail = "Le billet n'est valable que de 30 minutes avant le début du créneau jusqu'à sa fin.",
                    code = "OUTSIDE_ADMISSION_WINDOW",
                    title = "Hors fenêtre d'admission",
                    booking = BookingAccessResponse.from(result.view),
                )

            ValidateBookingAccessResult.NotAssignedServer ->
                problem(
                    HttpStatus.FORBIDDEN,
                    "Vous n'êtes pas rattaché à cette salle sur ce créneau.",
                    "NOT_ASSIGNED_SERVER",
                    "Contrôle non autorisé",
                )

            ValidateBookingAccessResult.BookingNotFound ->
                problem(
                    HttpStatus.NOT_FOUND,
                    "Aucune réservation ne correspond à ce billet.",
                    "BOOKING_NOT_FOUND",
                    "Réservation introuvable",
                )

            ValidateBookingAccessResult.RoomNotFound ->
                problem(HttpStatus.NOT_FOUND, "Salle introuvable.", "ROOM_NOT_FOUND", "Salle introuvable")
        }
    }

    private fun refusal(
        detail: String,
        code: String,
        title: String,
        booking: BookingAccessResponse,
    ): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail).apply {
                this.title = title
                setProperty("code", code)
                setProperty("booking", booking)
            },
        )

    private fun problem(
        status: HttpStatus,
        detail: String,
        code: String,
        title: String,
    ): ResponseEntity<Any> =
        ResponseEntity.status(status).body(
            ProblemDetail.forStatusAndDetail(status, detail).apply {
                this.title = title
                setProperty("code", code)
            },
        )
}
