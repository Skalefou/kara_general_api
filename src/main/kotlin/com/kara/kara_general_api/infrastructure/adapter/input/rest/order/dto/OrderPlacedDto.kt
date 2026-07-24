package com.kara.kara_general_api.infrastructure.adapter.input.rest.order.dto

import io.swagger.v3.oas.annotations.media.Schema

/** Charge utile d'une alerte « nouvelle commande » diffusée aux serveurs via STOMP. */
data class OrderPlacedDto(
    @field:Schema(description = "Identifiant de la commande")
    val orderId: String,
    @field:Schema(description = "Identifiant de la réservation concernée")
    val bookingId: String,
    @field:Schema(description = "Identifiant de la salle")
    val roomId: String,
    @field:Schema(description = "Nom du produit commandé")
    val productName: String,
    @field:Schema(description = "Quantité commandée")
    val quantity: Int,
    @field:Schema(description = "Montant total de la commande")
    val totalPrice: String,
    @field:Schema(description = "Devise (code ISO 4217)")
    val currency: String,
    @field:Schema(description = "Horodatage de la commande (ISO 8601, UTC)")
    val placedAt: String,
)
