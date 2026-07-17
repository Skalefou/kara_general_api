package com.kara.kara_general_api.domain.model.room

import java.math.BigDecimal

/**
 * Option tarifée d'une salle. Le prix est un **forfait fixe** : il ne dépend ni du nombre de
 * personnes, ni de la durée de la réservation.
 */
data class RoomOption(
    val id: RoomOptionId,
    val roomId: RoomId,
    val label: String,
    val description: String?,
    val price: BigDecimal,
    val currency: Currency,
) {
    init {
        require(label.isNotBlank()) { "Le libellé de l'option est obligatoire" }
        require(price >= BigDecimal.ZERO) { "Le prix de l'option doit être positif" }
    }
}
