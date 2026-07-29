package com.kara.kara_general_api.infrastructure.adapter.input.rest.pool

import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.pool.AddPoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.AddPoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.AddPoolShareUseCase
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareUseCase
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolCommand
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolResult
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolShareInput
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolUseCase
import com.kara.kara_general_api.domain.port.input.pool.GetPoolRecapResult
import com.kara.kara_general_api.domain.port.input.pool.GetPoolRecapUseCase
import com.kara.kara_general_api.domain.port.input.pool.GetPoolResult
import com.kara.kara_general_api.domain.port.input.pool.GetPoolUseCase
import com.kara.kara_general_api.domain.port.input.pool.ListUserPoolsUseCase
import com.kara.kara_general_api.domain.port.input.pool.RegeneratePoolLinkCommand
import com.kara.kara_general_api.domain.port.input.pool.RegeneratePoolLinkResult
import com.kara.kara_general_api.domain.port.input.pool.RegeneratePoolLinkUseCase
import com.kara.kara_general_api.domain.port.input.pool.RemindPoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.RemindPoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.RemindPoolShareUseCase
import com.kara.kara_general_api.domain.port.input.pool.SelfJoinPoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.SelfJoinPoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.SelfJoinPoolShareUseCase
import com.kara.kara_general_api.domain.port.input.pool.UpdatePoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.UpdatePoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.UpdatePoolShareUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.AddPoolShareRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.AuthorizePoolShareResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.CreatePoolRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.PoolRecapResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.PoolResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.PoolSummaryResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.RegeneratePoolLinkResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.SelfJoinPoolShareRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.UpdatePoolShareRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/pools")
class PoolController(
    private val createPoolUseCase: CreatePoolUseCase,
    private val getPoolUseCase: GetPoolUseCase,
    private val getPoolRecapUseCase: GetPoolRecapUseCase,
    private val authorizePoolShareUseCase: AuthorizePoolShareUseCase,
    private val addPoolShareUseCase: AddPoolShareUseCase,
    private val updatePoolShareUseCase: UpdatePoolShareUseCase,
    private val regeneratePoolLinkUseCase: RegeneratePoolLinkUseCase,
    private val remindPoolShareUseCase: RemindPoolShareUseCase,
    private val listUserPoolsUseCase: ListUserPoolsUseCase,
    private val selfJoinPoolShareUseCase: SelfJoinPoolShareUseCase,
) : PoolApi {
    override fun listPools(authentication: Authentication): ResponseEntity<Any> =
        ResponseEntity.ok(
            listUserPoolsUseCase.listForUser(callerId(authentication)).map { PoolSummaryResponse.from(it) },
        )

    override fun createPool(
        request: CreatePoolRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            CreatePoolCommand(
                bookingId = BookingId(request.bookingId),
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
        return when (val result = createPoolUseCase.create(command)) {
            is CreatePoolResult.Created ->
                ResponseEntity.status(HttpStatus.CREATED).body(PoolResponse.from(result.view))
            CreatePoolResult.BookingNotFound ->
                problem(HttpStatus.NOT_FOUND, "Réservation introuvable", "POOL_BOOKING_NOT_FOUND")
            CreatePoolResult.NotOwner -> notOwner()
            CreatePoolResult.BookingNotPending ->
                problem(HttpStatus.CONFLICT, "La réservation n'est plus en attente de paiement.", "POOL_BOOKING_NOT_PENDING")
            CreatePoolResult.NotSharedPot ->
                problem(HttpStatus.CONFLICT, "La réservation n'a pas été créée en mode cagnotte.", "POOL_BOOKING_NOT_SHARED_POT")
            CreatePoolResult.PoolAlreadyExists ->
                problem(HttpStatus.CONFLICT, "Une cagnotte existe déjà pour cette réservation.", "POOL_ALREADY_EXISTS")
            CreatePoolResult.ReservationTooClose ->
                problem(HttpStatus.CONFLICT, "La réservation débute trop tôt pour ouvrir une cagnotte.", "POOL_RESERVATION_TOO_CLOSE")
            is CreatePoolResult.SharesMismatch ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    problemDetail(
                        HttpStatus.BAD_REQUEST,
                        "La somme des parts (${result.actual}) doit égaler le prix total (${result.expected}).",
                        "POOL_SHARES_MISMATCH",
                    ).apply {
                        setProperty("expected", result.expected)
                        setProperty("actual", result.actual)
                    },
                )
            CreatePoolResult.InvalidShares ->
                problem(HttpStatus.BAD_REQUEST, "Les parts sont invalides (vides ou montant non positif).", "POOL_INVALID_SHARES")
        }
    }

    override fun getPool(
        id: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> = toResponse(getPoolUseCase.getById(PoolId(id), callerId(authentication)))

    override fun getPoolByBooking(
        bookingId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> = toResponse(getPoolUseCase.getByBookingId(BookingId(bookingId), callerId(authentication)))

    override fun getPoolByExtension(
        extensionId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> = toResponse(getPoolUseCase.getByExtensionId(BookingExtensionId(extensionId), callerId(authentication)))

    private fun toResponse(result: GetPoolResult): ResponseEntity<Any> =
        when (result) {
            is GetPoolResult.Found -> ResponseEntity.ok(PoolResponse.from(result.view))
            GetPoolResult.NotFound -> poolNotFound()
            GetPoolResult.NotOwner -> notOwner()
        }

    override fun joinRecap(globalToken: String): ResponseEntity<Any> = toRecapResponse(getPoolRecapUseCase.getByGlobalToken(globalToken))

    override fun shareRecap(uniqueToken: String): ResponseEntity<Any> = toRecapResponse(getPoolRecapUseCase.getByShareToken(uniqueToken))

    private fun toRecapResponse(result: GetPoolRecapResult): ResponseEntity<Any> =
        when (result) {
            is GetPoolRecapResult.Found -> ResponseEntity.ok(PoolRecapResponse.from(result.view))
            GetPoolRecapResult.NotFound -> poolNotFound()
        }

    override fun authorizeShare(
        poolId: UUID,
        shareId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            AuthorizePoolShareCommand(
                poolId = PoolId(poolId),
                shareId = PoolShareId(shareId),
                payerId = callerId(authentication),
            )
        return when (val result = authorizePoolShareUseCase.authorize(command)) {
            is AuthorizePoolShareResult.Ready -> ResponseEntity.ok(AuthorizePoolShareResponse.from(result))
            AuthorizePoolShareResult.PoolNotFound -> poolNotFound()
            AuthorizePoolShareResult.ShareNotFound -> shareNotFound()
            AuthorizePoolShareResult.PayerNotFound ->
                problem(HttpStatus.NOT_FOUND, "Payeur introuvable.", "POOL_PAYER_NOT_FOUND")
            AuthorizePoolShareResult.PoolClosed -> poolClosed()
            AuthorizePoolShareResult.PoolExpired ->
                problem(HttpStatus.CONFLICT, "Le délai de la cagnotte est écoulé.", "POOL_EXPIRED")
            AuthorizePoolShareResult.ShareAlreadyProcessed ->
                problem(HttpStatus.CONFLICT, "Cette part n'est plus à payer.", "POOL_SHARE_ALREADY_PROCESSED")
        }
    }

    override fun selfJoinShare(
        globalToken: String,
        request: SelfJoinPoolShareRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            SelfJoinPoolShareCommand(
                globalToken = globalToken,
                callerId = callerId(authentication),
                amount = request.amount,
            )
        return when (val result = selfJoinPoolShareUseCase.selfJoin(command)) {
            is SelfJoinPoolShareResult.Ready ->
                ResponseEntity.ok(
                    AuthorizePoolShareResponse(
                        shareId = result.shareId,
                        clientSecret = result.clientSecret,
                        ephemeralKeySecret = result.ephemeralKeySecret,
                        customerId = result.customerId,
                        publishableKey = result.publishableKey,
                    ),
                )
            SelfJoinPoolShareResult.PoolNotFound -> poolNotFound()
            SelfJoinPoolShareResult.PayerNotFound ->
                problem(HttpStatus.NOT_FOUND, "Payeur introuvable.", "POOL_PAYER_NOT_FOUND")
            SelfJoinPoolShareResult.PoolClosed -> poolClosed()
            SelfJoinPoolShareResult.PoolExpired ->
                problem(HttpStatus.CONFLICT, "Le délai de la cagnotte est écoulé.", "POOL_EXPIRED")
            SelfJoinPoolShareResult.AlreadyJoined ->
                problem(HttpStatus.CONFLICT, "Vous détenez déjà une part dans cette cagnotte.", "POOL_ALREADY_JOINED")
            SelfJoinPoolShareResult.RemainderLocked ->
                problem(
                    HttpStatus.CONFLICT,
                    "Le reliquat du créateur est déjà réglé : auto-inscription impossible.",
                    "POOL_REMAINDER_LOCKED",
                )
            SelfJoinPoolShareResult.NoCreatorRemainder ->
                problem(HttpStatus.CONFLICT, "Aucun reliquat créateur disponible pour financer cette part.", "POOL_NO_CREATOR_REMAINDER")
            SelfJoinPoolShareResult.InsufficientRemainder ->
                problem(HttpStatus.CONFLICT, "Le reliquat du créateur est insuffisant.", "POOL_INSUFFICIENT_REMAINDER")
            SelfJoinPoolShareResult.InvalidAmount ->
                problem(HttpStatus.BAD_REQUEST, "Montant de part invalide.", "POOL_INVALID_AMOUNT")
        }
    }

    override fun addShare(
        poolId: UUID,
        request: AddPoolShareRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            AddPoolShareCommand(
                poolId = PoolId(poolId),
                requesterId = callerId(authentication),
                participantName = request.participantName,
                email = request.email,
                amount = request.amount,
            )
        return when (val result = addPoolShareUseCase.addShare(command)) {
            is AddPoolShareResult.Added ->
                ResponseEntity.status(HttpStatus.CREATED).body(PoolResponse.from(result.view))
            AddPoolShareResult.PoolNotFound -> poolNotFound()
            AddPoolShareResult.NotOwner -> notOwner()
            AddPoolShareResult.PoolClosed -> poolClosed()
            AddPoolShareResult.NoCreatorRemainder ->
                problem(HttpStatus.CONFLICT, "Aucun reliquat créateur disponible pour financer cette part.", "POOL_NO_CREATOR_REMAINDER")
            AddPoolShareResult.InsufficientRemainder ->
                problem(HttpStatus.CONFLICT, "Le reliquat du créateur est insuffisant.", "POOL_INSUFFICIENT_REMAINDER")
            AddPoolShareResult.InvalidShare ->
                problem(HttpStatus.BAD_REQUEST, "Montant de part invalide.", "POOL_INVALID_SHARE")
        }
    }

    override fun updateShare(
        poolId: UUID,
        shareId: UUID,
        request: UpdatePoolShareRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            UpdatePoolShareCommand(
                poolId = PoolId(poolId),
                shareId = PoolShareId(shareId),
                requesterId = callerId(authentication),
                newAmount = request.amount,
            )
        return when (val result = updatePoolShareUseCase.updateShare(command)) {
            is UpdatePoolShareResult.Updated -> ResponseEntity.ok(PoolResponse.from(result.view))
            UpdatePoolShareResult.PoolNotFound -> poolNotFound()
            UpdatePoolShareResult.NotOwner -> notOwner()
            UpdatePoolShareResult.ShareNotFound -> shareNotFound()
            UpdatePoolShareResult.PoolClosed -> poolClosed()
            UpdatePoolShareResult.ShareAlreadyPaid ->
                problem(HttpStatus.CONFLICT, "Cette part est déjà réglée et ne peut plus être modifiée.", "POOL_SHARE_ALREADY_PAID")
            UpdatePoolShareResult.CannotEditCreatorShare ->
                problem(
                    HttpStatus.CONFLICT,
                    "Le reliquat du créateur ne peut pas être modifié directement.",
                    "POOL_CANNOT_EDIT_CREATOR_SHARE",
                )
            UpdatePoolShareResult.CreatorShareLocked ->
                problem(
                    HttpStatus.CONFLICT,
                    "Le reliquat du créateur est déjà réglé : rééquilibrage impossible.",
                    "POOL_CREATOR_SHARE_LOCKED",
                )
            UpdatePoolShareResult.InsufficientRemainder ->
                problem(HttpStatus.CONFLICT, "Le rééquilibrage rendrait le reliquat du créateur négatif.", "POOL_INSUFFICIENT_REMAINDER")
            UpdatePoolShareResult.InvalidAmount ->
                problem(HttpStatus.BAD_REQUEST, "Montant invalide.", "POOL_INVALID_AMOUNT")
        }
    }

    override fun regenerateLink(
        poolId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command = RegeneratePoolLinkCommand(poolId = PoolId(poolId), requesterId = callerId(authentication))
        return when (val result = regeneratePoolLinkUseCase.regenerate(command)) {
            is RegeneratePoolLinkResult.Regenerated ->
                ResponseEntity.ok(RegeneratePoolLinkResponse.from(result))
            RegeneratePoolLinkResult.PoolNotFound -> poolNotFound()
            RegeneratePoolLinkResult.NotOwner -> notOwner()
            RegeneratePoolLinkResult.PoolClosed -> poolClosed()
        }
    }

    override fun remindShare(
        poolId: UUID,
        shareId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        val command =
            RemindPoolShareCommand(
                poolId = PoolId(poolId),
                shareId = PoolShareId(shareId),
                requesterId = callerId(authentication),
            )
        return when (remindPoolShareUseCase.remind(command)) {
            RemindPoolShareResult.Reminded -> ResponseEntity.noContent().build()
            RemindPoolShareResult.PoolNotFound -> poolNotFound()
            RemindPoolShareResult.NotOwner -> notOwner()
            RemindPoolShareResult.ShareNotFound -> shareNotFound()
            RemindPoolShareResult.NoEmail ->
                problem(HttpStatus.CONFLICT, "Cette part n'a pas d'email : relance impossible.", "POOL_SHARE_NO_EMAIL")
            RemindPoolShareResult.AlreadyPaid ->
                problem(HttpStatus.CONFLICT, "Cette part est déjà réglée.", "POOL_SHARE_ALREADY_PAID")
        }
    }

    private fun callerId(authentication: Authentication): UserId = UserId(UUID.fromString(authentication.name))

    private fun poolNotFound(): ResponseEntity<Any> = problem(HttpStatus.NOT_FOUND, "Aucune cagnotte ne correspond.", "POOL_NOT_FOUND")

    private fun shareNotFound(): ResponseEntity<Any> =
        problem(HttpStatus.NOT_FOUND, "Aucune part ne correspond à cette cagnotte.", "POOL_SHARE_NOT_FOUND")

    private fun notOwner(): ResponseEntity<Any> =
        problem(HttpStatus.FORBIDDEN, "Cette cagnotte n'appartient pas à votre compte.", "POOL_NOT_OWNER")

    private fun poolClosed(): ResponseEntity<Any> = problem(HttpStatus.CONFLICT, "La cagnotte n'est plus ouverte.", "POOL_CLOSED")

    private fun problem(
        status: HttpStatus,
        detail: String,
        code: String,
    ): ResponseEntity<Any> = ResponseEntity.status(status).body(problemDetail(status, detail, code))

    private fun problemDetail(
        status: HttpStatus,
        detail: String,
        code: String,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            title = code.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
            setProperty("code", code)
        }
}
