package com.kara.kara_general_api.infrastructure.adapter.input.rest.order

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.order.GetBookingMenuCommand
import com.kara.kara_general_api.domain.port.input.order.GetBookingMenuResult
import com.kara.kara_general_api.domain.port.input.order.GetBookingMenuUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.order.dto.AvailableProductResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/available-products")
class BookingMenuController(
    private val getBookingMenuUseCase: GetBookingMenuUseCase,
) : BookingMenuApi {

    @GetMapping
    override fun getAvailableProducts(
        bookingId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            GetBookingMenuCommand(
                bookingId = BookingId(bookingId),
                currentUserId = UserId(UUID.fromString(authentication.name)),
            )
        return when (val result = getBookingMenuUseCase.getBookingMenu(command)) {
            is GetBookingMenuResult.Success ->
                ResponseEntity.ok(result.entries.map { AvailableProductResponse.from(it) })
            GetBookingMenuResult.BookingNotFound -> bookingNotFound()
            GetBookingMenuResult.NotOwner -> bookingNotOwner()
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

    private fun bookingNotOwner(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Cette réservation n'appartient pas à l'utilisateur courant.",
            ).apply {
                title = "Accès refusé"
                setProperty("code", "BOOKING_NOT_OWNER")
            },
        )
}
