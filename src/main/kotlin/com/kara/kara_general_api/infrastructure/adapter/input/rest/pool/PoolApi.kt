package com.kara.kara_general_api.infrastructure.adapter.input.rest.pool

import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.AddPoolShareRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.AuthorizePoolShareResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.CreatePoolRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.PoolRecapResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.PoolResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.PoolSummaryResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.RegeneratePoolLinkResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.SelfJoinPoolShareRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto.UpdatePoolShareRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(
    name = "Cagnottes",
    description =
        "Cagnotte partagée (règlement Stripe en autorisation à capture manuelle) : la carte de " +
            "chaque participant n'est prélevée que lorsque toutes les parts ont été payées, sinon la cagnotte " +
            "est annulée et personne n'est prélevé.",
)
interface PoolApi {
    @Operation(
        summary = "Créer une cagnotte",
        description =
            "Ouvre une cagnotte pour une réservation PENDING en mode sharedPot appartenant au client. " +
                "La somme des parts (reliquat du créateur inclus) doit égaler le prix total de la réservation.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Cagnotte créée",
                content = [Content(schema = Schema(implementation = PoolResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description =
                    "Somme des parts != prix total (POOL_SHARES_MISMATCH) ou parts invalides " +
                        "(POOL_INVALID_SHARES)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(responseCode = "403", description = "Réservation appartenant à un autre client (POOL_NOT_OWNER)"),
            ApiResponse(responseCode = "404", description = "Réservation introuvable (POOL_BOOKING_NOT_FOUND)"),
            ApiResponse(
                responseCode = "409",
                description =
                    "Réservation non PENDING (POOL_BOOKING_NOT_PENDING), non sharedPot " +
                        "(POOL_BOOKING_NOT_SHARED_POT), cagnotte déjà existante (POOL_ALREADY_EXISTS) ou réservation " +
                        "trop proche (POOL_RESERVATION_TOO_CLOSE)",
            ),
        ],
    )
    @PostMapping
    fun createPool(
        @Valid @RequestBody request: CreatePoolRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Lister mes cagnottes",
        description =
            "Cagnottes de l'utilisateur authentifié (« Mes événements ») : celles qu'il a créées OU " +
                "dont il détient une part. Résumés triés par échéance décroissante.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste des cagnottes de l'utilisateur",
                content = [Content(array = ArraySchema(schema = Schema(implementation = PoolSummaryResponse::class)))],
            ),
        ],
    )
    @GetMapping
    fun listPools(authentication: Authentication): ResponseEntity<Any>

    @Operation(
        summary = "Statut d'une cagnotte par identifiant",
        description = "Statut complet pour l'écran « Gérer l'événement ». Réservé au créateur.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content = [Content(schema = Schema(implementation = PoolResponse::class))],
            ),
            ApiResponse(responseCode = "403", description = "POOL_NOT_OWNER"),
            ApiResponse(responseCode = "404", description = "POOL_NOT_FOUND"),
        ],
    )
    @GetMapping("/{id}")
    fun getPool(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Statut d'une cagnotte par réservation",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content = [Content(schema = Schema(implementation = PoolResponse::class))],
            ),
            ApiResponse(responseCode = "403", description = "POOL_NOT_OWNER"),
            ApiResponse(responseCode = "404", description = "POOL_NOT_FOUND"),
        ],
    )
    @GetMapping("/by-booking/{bookingId}")
    fun getPoolByBooking(
        @PathVariable bookingId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Cagnotte d'une extension de réservation",
        description = "Statut de la cagnotte ouverte pour une prolongation, réservé au client propriétaire.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Cagnotte trouvée",
                content = [Content(schema = Schema(implementation = PoolResponse::class))],
            ),
            ApiResponse(responseCode = "403", description = "POOL_NOT_OWNER"),
            ApiResponse(responseCode = "404", description = "POOL_NOT_FOUND"),
        ],
    )
    @GetMapping("/by-extension/{extensionId}")
    fun getPoolByExtension(
        @PathVariable extensionId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Récapitulatif public via le lien global",
        description =
            "Lecture publique : résumé de la réservation, progression de la cagnotte et message de débit " +
                "différé.\n\n" +
                "**Authentification facultative.** Un en-tête `Authorization: Bearer <JWT>` absent, expiré ou " +
                "invalide n'est **pas** une erreur : la réponse est simplement celle d'un invité (champ `share` " +
                "absent), jamais un 401.\n\n" +
                "Avec un JWT valide, `share` contient **la part dont l'appelant est le payeur**, et elle seule : " +
                "le filtrage est fait sur le payeur côté serveur, la part d'un autre participant n'est jamais " +
                "exposée. Si l'appelant ne détient aucune part, `share` est absent.\n\n" +
                "Cas d'usage : **reprise d'un paiement interrompu**. Quand le PaymentSheet échoue après " +
                "`POST /api/v1/pools/join/{globalToken}/shares`, la part existe déjà et une seconde " +
                "auto-inscription est refusée (POOL_ALREADY_JOINED). Cet endpoint restitue le `shareId` " +
                "nécessaire pour relancer le paiement via " +
                "`POST /api/v1/pools/{poolId}/shares/{shareId}/payment`, puis `.../sync`.\n\n" +
                "Le paiement d'une part, lui, requiert toujours une authentification.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description =
                    "Récapitulatif de la cagnotte. `share` est renseigné uniquement si la requête porte un " +
                        "JWT valide dont le porteur détient une part de cette cagnotte.",
                content = [Content(schema = Schema(implementation = PoolRecapResponse::class))],
            ),
            ApiResponse(responseCode = "404", description = "POOL_NOT_FOUND"),
        ],
    )
    @GetMapping("/join/{globalToken}")
    fun joinRecap(
        @PathVariable globalToken: String,
        // Nullable OBLIGATOIREMENT : sans authentification, Spring passe null ici. Un type non-nullable ferait
        // échouer l'invocation Kotlin de la méthode du contrôleur (500 systématique pour les invités).
        authentication: Authentication?,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Récapitulatif public via le lien d'une part",
        description = "Lecture publique : idem lien global, plus le détail de la part concernée.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content = [Content(schema = Schema(implementation = PoolRecapResponse::class))],
            ),
            ApiResponse(responseCode = "404", description = "POOL_NOT_FOUND"),
        ],
    )
    @GetMapping("/share/{uniqueToken}")
    fun shareRecap(
        @PathVariable uniqueToken: String,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Autoriser (payer) une part",
        description =
            "Crée un PaymentIntent Stripe en capture manuelle pour bloquer le montant de la part " +
                "(aucun prélèvement immédiat) et retourne les secrets du PaymentSheet. La capture n'a lieu qu'une " +
                "fois toute la cagnotte complète.\n\n" +
                "Ouvert à **tout utilisateur authentifié** : il n'est pas nécessaire d'être le créateur de la " +
                "cagnotte (un participant invité par lien ne l'est pas). Le créateur règle sa **part de reliquat** " +
                "(la part `isCreatorShare` de `GET /api/v1/pools/{id}`) par ce même endpoint, sans traitement " +
                "particulier : il suffit de lui passer le `shareId` de cette part.\n\n" +
                "Préconditions : cagnotte OPEN et non expirée, part PENDING. Le montant autorisé est le montant " +
                "**courant** de la part ; si ce montant change ensuite (un participant rejoint et découpe le " +
                "reliquat), l'autorisation en cours est libérée et détachée, et la part doit être réglée à nouveau " +
                "au nouveau montant. Après le PaymentSheet, appeler " +
                "`POST /api/v1/pools/{poolId}/shares/{shareId}/sync` pour ne pas dépendre du seul webhook.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content = [Content(schema = Schema(implementation = AuthorizePoolShareResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Requête non authentifiée"),
            ApiResponse(responseCode = "404", description = "POOL_NOT_FOUND / POOL_SHARE_NOT_FOUND / POOL_PAYER_NOT_FOUND"),
            ApiResponse(
                responseCode = "409",
                description =
                    "Cagnotte fermée (POOL_CLOSED), expirée (POOL_EXPIRED) ou part déjà traitée " +
                        "(POOL_SHARE_ALREADY_PROCESSED)",
            ),
        ],
    )
    @PostMapping("/{poolId}/shares/{shareId}/payment")
    fun authorizeShare(
        @PathVariable poolId: UUID,
        @PathVariable shareId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Réconcilier le paiement d'une part",
        description =
            "Interroge Stripe pour connaître le statut réel du PaymentIntent de la part. Si les fonds sont " +
                "bloqués (`requires_capture`), la part passe AUTHORIZED et, si la cagnotte devient complète, toutes " +
                "les autorisations sont capturées et la réservation passe CONFIRMED — mêmes effets que le webhook, " +
                "de façon idempotente. Si l'intent est annulé, la part passe CANCELLED ; sinon rien n'est modifié. " +
                "À appeler par le front après le PaymentSheet pour ne pas dépendre de la seule arrivée du webhook. " +
                "Retourne l'état à jour de la part ET de la cagnotte (progression), de quoi rafraîchir l'écran sans " +
                "second appel. Réservé au payeur de la part ou au créateur de la cagnotte.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "État à jour de la part et de la cagnotte",
                content = [Content(schema = Schema(implementation = PoolRecapResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Requête non authentifiée"),
            ApiResponse(
                responseCode = "403",
                description = "Ni payeur de la part ni créateur de la cagnotte (POOL_SHARE_NOT_ALLOWED)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Cagnotte introuvable (POOL_NOT_FOUND) ou part introuvable (POOL_SHARE_NOT_FOUND)",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))],
            ),
        ],
    )
    @PostMapping("/{poolId}/shares/{shareId}/sync")
    fun syncShare(
        @PathVariable poolId: UUID,
        @PathVariable shareId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Rejoindre une cagnotte (auto-inscription via le lien global)",
        description =
            "Un utilisateur authentifié crée sa propre part via le lien global : le montant (plafonné " +
                "par le reliquat du créateur) est prélevé sur ce reliquat, puis un PaymentIntent Stripe en capture " +
                "manuelle est créé. Nom et email de la part sont dérivés du compte. Retourne les secrets du " +
                "PaymentSheet. Une seule part par personne.\n\n" +
                "La découpe libère l'éventuelle autorisation Stripe encore en cours sur le reliquat (le créateur " +
                "avait ouvert son PaymentSheet sans que le blocage soit enregistré) : elle ne couvrirait plus le " +
                "nouveau montant dû. Le créateur doit alors régler son reliquat à nouveau. Si son reliquat est " +
                "déjà AUTHORIZED/CAPTURED, la découpe est refusée (POOL_REMAINDER_LOCKED).",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content = [Content(schema = Schema(implementation = AuthorizePoolShareResponse::class))],
            ),
            ApiResponse(responseCode = "400", description = "Montant invalide (POOL_INVALID_AMOUNT)"),
            ApiResponse(responseCode = "401", description = "Requête non authentifiée"),
            ApiResponse(
                responseCode = "404",
                description = "Cagnotte introuvable (POOL_NOT_FOUND) ou payeur introuvable (POOL_PAYER_NOT_FOUND)",
            ),
            ApiResponse(
                responseCode = "409",
                description =
                    "Cagnotte fermée (POOL_CLOSED), expirée (POOL_EXPIRED), déjà rejointe " +
                        "(POOL_ALREADY_JOINED), reliquat verrouillé (POOL_REMAINDER_LOCKED), absent " +
                        "(POOL_NO_CREATOR_REMAINDER) ou insuffisant (POOL_INSUFFICIENT_REMAINDER)",
            ),
        ],
    )
    @PostMapping("/join/{globalToken}/shares")
    fun selfJoinShare(
        @PathVariable globalToken: String,
        @Valid @RequestBody request: SelfJoinPoolShareRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Ajouter un participant (invitation par email)",
        description =
            "Le créateur ajoute une part par email ; le montant est prélevé sur son reliquat. Un lien " +
                "unique est généré et une invitation est envoyée.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                content = [Content(schema = Schema(implementation = PoolResponse::class))],
            ),
            ApiResponse(responseCode = "400", description = "POOL_INVALID_SHARE"),
            ApiResponse(responseCode = "403", description = "POOL_NOT_OWNER"),
            ApiResponse(responseCode = "404", description = "POOL_NOT_FOUND"),
            ApiResponse(
                responseCode = "409",
                description =
                    "Cagnotte fermée (POOL_CLOSED), pas de reliquat créateur (POOL_NO_CREATOR_REMAINDER) " +
                        "ou reliquat insuffisant (POOL_INSUFFICIENT_REMAINDER)",
            ),
        ],
    )
    @PostMapping("/{poolId}/shares")
    fun addShare(
        @PathVariable poolId: UUID,
        @Valid @RequestBody request: AddPoolShareRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Modifier le montant d'une part non payée",
        description = "L'écart est répercuté sur le reliquat du créateur. Rejeté si la part est déjà autorisée/capturée.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content = [Content(schema = Schema(implementation = PoolResponse::class))],
            ),
            ApiResponse(responseCode = "400", description = "POOL_INVALID_AMOUNT"),
            ApiResponse(responseCode = "403", description = "POOL_NOT_OWNER"),
            ApiResponse(responseCode = "404", description = "POOL_NOT_FOUND / POOL_SHARE_NOT_FOUND"),
            ApiResponse(
                responseCode = "409",
                description =
                    "Cagnotte fermée (POOL_CLOSED), part déjà payée (POOL_SHARE_ALREADY_PAID), reliquat " +
                        "créateur (POOL_CANNOT_EDIT_CREATOR_SHARE), reliquat verrouillé (POOL_CREATOR_SHARE_LOCKED) ou " +
                        "insuffisant (POOL_INSUFFICIENT_REMAINDER)",
            ),
        ],
    )
    @PatchMapping("/{poolId}/shares/{shareId}")
    fun updateShare(
        @PathVariable poolId: UUID,
        @PathVariable shareId: UUID,
        @Valid @RequestBody request: UpdatePoolShareRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Régénérer le lien global",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                content = [Content(schema = Schema(implementation = RegeneratePoolLinkResponse::class))],
            ),
            ApiResponse(responseCode = "403", description = "POOL_NOT_OWNER"),
            ApiResponse(responseCode = "404", description = "POOL_NOT_FOUND"),
            ApiResponse(responseCode = "409", description = "Cagnotte fermée (POOL_CLOSED)"),
        ],
    )
    @PostMapping("/{poolId}/regenerate-link")
    fun regenerateLink(
        @PathVariable poolId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Relancer un participant",
        description = "Renvoie l'invitation par email (et push si un token d'appareil est connu).",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Relance envoyée"),
            ApiResponse(responseCode = "403", description = "POOL_NOT_OWNER"),
            ApiResponse(responseCode = "404", description = "POOL_NOT_FOUND / POOL_SHARE_NOT_FOUND"),
            ApiResponse(
                responseCode = "409",
                description = "Part sans email (POOL_SHARE_NO_EMAIL) ou déjà payée (POOL_SHARE_ALREADY_PAID)",
            ),
        ],
    )
    @PostMapping("/{poolId}/shares/{shareId}/remind")
    fun remindShare(
        @PathVariable poolId: UUID,
        @PathVariable shareId: UUID,
        authentication: Authentication,
    ): ResponseEntity<Any>
}
