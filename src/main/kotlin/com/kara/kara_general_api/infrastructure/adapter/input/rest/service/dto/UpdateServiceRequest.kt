package com.kara.kara_general_api.infrastructure.adapter.input.rest.service.dto

import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import java.math.BigDecimal

/**
 * Mise à jour partielle d'un service : tout champ omis (null) laisse la valeur existante inchangée.
 */
data class UpdateServiceRequest(
    @field:Schema(description = "Libellé du service", example = "Ménage fin de soirée")
    val label: String? = null,
    @field:Schema(description = "Description du service", example = "Nettoyage complet après l'événement")
    val description: String? = null,
    @field:DecimalMin(value = "0.0", message = "Le prix du service doit être positif")
    @field:Schema(description = "Prix forfaitaire fixe du service", example = "60.00")
    val price: BigDecimal? = null,
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency? = null,
)
