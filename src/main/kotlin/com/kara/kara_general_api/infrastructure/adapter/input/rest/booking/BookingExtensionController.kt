package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking

import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.ExtendBookingCommand
import com.kara.kara_general_api.domain.port.input.booking.ExtendBookingResult
import com.kara.kara_general_api.domain.port.input.booking.ExtendBookingUseCase
import com.kara.kara_general_api.domain.port.input.booking.GetExtensionOptionsCommand
import com.kara.kara_general_api.domain.port.input.booking.GetExtensionOptionsResult
import com.kara.kara_general_api.domain.port.input.booking.GetExtensionOptionsUseCase
import com.kara.kara_general_api.domain.port.input.payment.InitiateExtensionPaymentCommand
import com.kara.kara_general_api.domain.port.input.payment.InitiateExtensionPaymentResult
import com.kara.kara_general_api.domain.port.input.payment.InitiateExtensionPaymentUseCase
import com.kara.kara_general_api.domain.port.input.pool.CreateExtensionPoolCommand
import com.kara.kara_general_api.domain.port.input.pool.CreateExtensionPoolResult
import com.kara.kara_general_api.domain.port.input.pool.CreateExtensionPoolUseCase
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolShareInput
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.BookingExtensionResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.CreateExtensionRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto.ExtensionOptionsResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.payment.dto.InitiateBookingPaymentResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.CreateExtensionPoolRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.PoolResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class BookingExtensionController(
    private val getExtensionOptionsUseCase: GetExtensionOptionsUseCase,
    private val extendBookingUseCase: ExtendBookingUseCase,
    private val initiateExtensionPaymentUseCase: InitiateExtensionPaymentUseCase,
    private val createExtensionPoolUseCase: CreateExtensionPoolUseCase,
) : BookingExtensionApi {
    @GetMapping("/bookings/{bookingId}/extension-options")
    override fun getExtensionOptions(
        @PathVariable bookingId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            GetExtensionOptionsCommand(
                bookingId = BookingId(bookingId),
                currentUserId = callerId(authentication),
            )
        return when (val result = getExtensionOptionsUseCase.getOptions(command)) {
            is GetExtensionOptionsResult.Success ->
                ResponseEntity.ok(ExtensionOptionsResponse.from(result.options))
            GetExtensionOptionsResult.BookingNotFound -> bookingNotFound()
            GetExtensionOptionsResult.NotOwner -> notOwner()
            GetExtensionOptionsResult.BookingNotConfirmed ->
                problem(HttpStatus.CONFLICT, "La réservation n'est pas confirmée.", "BOOKING_NOT_CONFIRMED")
            GetExtensionOptionsResult.BookingNotActive ->
                problem(
                    HttpStatus.CONFLICT,
                    "Une extension ne peut être demandée que pendant la réservation.",
                    "BOOKING_NOT_ACTIVE",
                )
            GetExtensionOptionsResult.ExtensionAlreadyPending ->
                problem(
                    HttpStatus.CONFLICT,
                    "Une extension est déjà en attente de paiement pour cette réservation.",
                    "EXTENSION_ALREADY_PENDING",
                )
            GetExtensionOptionsResult.RoomNotFound ->
                problem(HttpStatus.NOT_FOUND, "Salle introuvable.", "ROOM_NOT_FOUND")
        }
    }

    @PostMapping("/bookings/{bookingId}/extensions")
    override fun createExtension(
        @PathVariable bookingId: UUID,
        @Valid @RequestBody request: CreateExtensionRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val paymentMode =
            runCatching { PaymentMode.valueOf(request.paymentMode!!) }.getOrNull()
                ?: return problem(
                    HttpStatus.BAD_REQUEST,
                    "Mode de règlement invalide (PAY_ALL ou SHARED_POT).",
                    "INVALID_PAYMENT_MODE",
                )

        val command =
            ExtendBookingCommand(
                bookingId = BookingId(bookingId),
                currentUserId = callerId(authentication),
                additionalMinutes = request.additionalMinutes!!,
                paymentMode = paymentMode,
            )
        return when (val result = extendBookingUseCase.extend(command)) {
            is ExtendBookingResult.Created ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(BookingExtensionResponse.from(result.extension))
            ExtendBookingResult.BookingNotFound -> bookingNotFound()
            ExtendBookingResult.NotOwner -> notOwner()
            ExtendBookingResult.BookingNotConfirmed ->
                problem(HttpStatus.CONFLICT, "La réservation n'est pas confirmée.", "BOOKING_NOT_CONFIRMED")
            ExtendBookingResult.BookingNotActive ->
                problem(
                    HttpStatus.CONFLICT,
                    "Une extension ne peut être demandée que pendant la réservation.",
                    "BOOKING_NOT_ACTIVE",
                )
            ExtendBookingResult.ExtensionAlreadyPending ->
                problem(
                    HttpStatus.CONFLICT,
                    "Une extension est déjà en attente de paiement pour cette réservation.",
                    "EXTENSION_ALREADY_PENDING",
                )
            ExtendBookingResult.RoomNotFound ->
                problem(HttpStatus.NOT_FOUND, "Salle introuvable.", "ROOM_NOT_FOUND")
            ExtendBookingResult.SettlementWindowTooShort ->
                problem(
                    HttpStatus.CONFLICT,
                    "Il ne reste plus assez de temps avant la fin de la réservation pour régler une extension.",
                    "EXTENSION_WINDOW_TOO_SHORT",
                )
            is ExtendBookingResult.SlotUnavailable ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ProblemDetail
                        .forStatusAndDetail(
                            HttpStatus.CONFLICT,
                            "La durée demandée n'est pas disponible sur ce créneau.",
                        ).apply {
                            title = "Créneau indisponible"
                            setProperty("code", "EXTENSION_SLOT_UNAVAILABLE")
                            setProperty("maxAdditionalMinutes", result.maxAdditionalMinutes)
                        },
                )
        }
    }

    @PostMapping("/extensions/{extensionId}/payment")
    override fun payExtension(
        @PathVariable extensionId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            InitiateExtensionPaymentCommand(
                extensionId = BookingExtensionId(extensionId),
                userId = callerId(authentication),
            )
        return when (val result = initiateExtensionPaymentUseCase.initiate(command)) {
            is InitiateExtensionPaymentResult.Ready ->
                ResponseEntity.ok(InitiateBookingPaymentResponse.from(result))
            InitiateExtensionPaymentResult.ExtensionNotFound -> extensionNotFound()
            InitiateExtensionPaymentResult.NotOwner -> notOwner()
            InitiateExtensionPaymentResult.AlreadySettled ->
                problem(HttpStatus.CONFLICT, "Cette extension n'est plus en attente de paiement.", "EXTENSION_NOT_PENDING")
            InitiateExtensionPaymentResult.ExtensionExpired ->
                problem(HttpStatus.CONFLICT, "Le délai de paiement de cette extension est échu.", "EXTENSION_EXPIRED")
            InitiateExtensionPaymentResult.NotPayAll ->
                problem(HttpStatus.CONFLICT, "Cette extension est réglée par cagnotte.", "EXTENSION_NOT_PAY_ALL")
        }
    }

    @PostMapping("/extensions/{extensionId}/pool")
    override fun createExtensionPool(
        @PathVariable extensionId: UUID,
        @Valid @RequestBody request: CreateExtensionPoolRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            CreateExtensionPoolCommand(
                extensionId = BookingExtensionId(extensionId),
                creatorId = callerId(authentication),
                shares =
                    request.shares.map {
                        CreatePoolShareInput(
                            participantName = it.participantName,
                            email = it.email,
                            amount = it.amount,
                            isCreatorShare = it.isCreatorShare,
                        )
                    },
            )
        return when (val result = createExtensionPoolUseCase.createForExtension(command)) {
            is CreateExtensionPoolResult.Created ->
                ResponseEntity.status(HttpStatus.CREATED).body(PoolResponse.from(result.view))
            CreateExtensionPoolResult.ExtensionNotFound -> extensionNotFound()
            CreateExtensionPoolResult.NotOwner -> notOwner()
            CreateExtensionPoolResult.ExtensionNotPending ->
                problem(HttpStatus.CONFLICT, "Cette extension n'est plus en attente de paiement.", "EXTENSION_NOT_PENDING")
            CreateExtensionPoolResult.NotSharedPot ->
                problem(HttpStatus.CONFLICT, "Cette extension n'a pas été créée en mode cagnotte.", "EXTENSION_NOT_SHARED_POT")
            CreateExtensionPoolResult.PoolAlreadyExists ->
                problem(HttpStatus.CONFLICT, "Une cagnotte existe déjà pour cette extension.", "POOL_ALREADY_EXISTS")
            CreateExtensionPoolResult.SettlementWindowTooShort ->
                problem(
                    HttpStatus.CONFLICT,
                    "Il ne reste plus assez de temps avant la fin de la réservation pour régler une cagnotte.",
                    "EXTENSION_WINDOW_TOO_SHORT",
                )
            CreateExtensionPoolResult.InvalidShares ->
                problem(HttpStatus.BAD_REQUEST, "Parts invalides.", "POOL_INVALID_SHARES")
            is CreateExtensionPoolResult.SharesMismatch ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ProblemDetail
                        .forStatusAndDetail(
                            HttpStatus.BAD_REQUEST,
                            "La somme des parts doit égaler le prix de l'extension.",
                        ).apply {
                            title = "Parts incohérentes"
                            setProperty("code", "POOL_SHARES_MISMATCH")
                            setProperty("expected", result.expected)
                            setProperty("actual", result.actual)
                        },
                )
        }
    }

    private fun callerId(authentication: Authentication): UserId = UserId(UUID.fromString(authentication.name))

    private fun bookingNotFound(): ResponseEntity<Any> =
        problem(HttpStatus.NOT_FOUND, "Aucune réservation ne correspond à cet identifiant.", "BOOKING_NOT_FOUND")

    private fun extensionNotFound(): ResponseEntity<Any> =
        problem(HttpStatus.NOT_FOUND, "Aucune extension ne correspond à cet identifiant.", "EXTENSION_NOT_FOUND")

    private fun notOwner(): ResponseEntity<Any> =
        problem(HttpStatus.FORBIDDEN, "Cette ressource n'appartient pas à l'utilisateur courant.", "NOT_OWNER")

    private fun problem(
        status: HttpStatus,
        detail: String,
        code: String,
    ): ResponseEntity<Any> =
        ResponseEntity.status(status).body(
            ProblemDetail.forStatusAndDetail(status, detail).apply {
                title = status.reasonPhrase
                setProperty("code", code)
            },
        )
}
