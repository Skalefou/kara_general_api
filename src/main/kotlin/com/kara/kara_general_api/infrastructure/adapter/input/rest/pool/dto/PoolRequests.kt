package com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.util.UUID

/** Corps de création d'une cagnotte. La somme des parts doit égaler le prix total de la réservation. */
data class CreatePoolRequest(
    @field:NotNull
    @field:Schema(description = "Réservation PENDING en mode sharedPot pour laquelle ouvrir la cagnotte")
    val bookingId: UUID,
    @field:NotEmpty
    @field:Valid
    @field:Schema(description = "Parts de la cagnotte (le reliquat du créateur est une part avec isCreatorShare=true)")
    val shares: List<CreatePoolShareRequest>,
)

data class CreatePoolShareRequest(
    @field:NotBlank
    @field:Schema(description = "Nom du participant", example = "Alice")
    val participantName: String,
    @field:Email
    @field:Schema(description = "Email du participant (optionnel). Si fourni, un lien unique est généré et envoyé.")
    val email: String? = null,
    @field:NotNull
    @field:Positive
    @field:Schema(description = "Montant de la part", example = "50.00")
    val amount: BigDecimal,
    @field:Schema(description = "Vrai s'il s'agit du reliquat du créateur", example = "false")
    val isCreatorShare: Boolean = false,
)

/** Corps d'ajout d'un participant par le créateur (le montant est prélevé sur le reliquat du créateur). */
data class AddPoolShareRequest(
    @field:NotBlank
    @field:Schema(description = "Nom du participant invité", example = "Bob")
    val participantName: String,
    @field:NotBlank
    @field:Email
    @field:Schema(description = "Email du participant invité", example = "bob@example.com")
    val email: String,
    @field:NotNull
    @field:Positive
    @field:Schema(description = "Montant de la part", example = "40.00")
    val amount: BigDecimal,
)

/** Corps de modification du montant d'une part non encore payée. */
data class UpdatePoolShareRequest(
    @field:NotNull
    @field:Positive
    @field:Schema(description = "Nouveau montant de la part", example = "60.00")
    val amount: BigDecimal,
)
