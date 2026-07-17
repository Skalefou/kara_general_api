package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto

import com.kara.kara_general_api.domain.model.booking.BookingEstimate
import com.kara.kara_general_api.domain.model.room.Currency
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class EstimateBreakdownResponse(
    @field:Schema(description = "Sous-total lié à la salle (prix/personne/heure × personnes × heures)", example = "300.00")
    val base: BigDecimal,
    @field:Schema(description = "Sous-total des options retenues (forfaits fixes)", example = "85.00")
    val options: BigDecimal,
)

data class EstimateBookingResponse(
    @field:Schema(description = "Prix total estimé (base + options)", example = "385.00")
    val totalPrice: BigDecimal,
    @field:Schema(description = "Prix par personne (total / nombre de personnes, arrondi 2 décimales)", example = "48.13")
    val pricePerPerson: BigDecimal,
    @field:Schema(description = "Devise (code ISO 4217)", example = "EUR")
    val currency: Currency,
    @field:Schema(description = "Détail du calcul")
    val breakdown: EstimateBreakdownResponse,
) {
    companion object {
        fun from(estimate: BookingEstimate): EstimateBookingResponse =
            EstimateBookingResponse(
                totalPrice = estimate.totalPrice,
                pricePerPerson = estimate.pricePerPerson,
                currency = estimate.currency,
                breakdown =
                    EstimateBreakdownResponse(
                        base = estimate.base,
                        options = estimate.optionsTotal,
                    ),
            )
    }
}
