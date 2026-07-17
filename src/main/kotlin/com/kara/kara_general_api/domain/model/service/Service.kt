package com.kara.kara_general_api.domain.model.service

import com.kara.kara_general_api.domain.model.room.Currency
import java.math.BigDecimal

/**
 * Service réutilisable du catalogue global. Le prix est un **forfait fixe** : il ne dépend ni du
 * nombre de personnes, ni de la durée de la réservation. Un service peut être attaché à plusieurs
 * salles (liaison salle↔service), indépendamment de toute salle en particulier.
 */
data class Service(
    val id: ServiceId,
    val label: String,
    val description: String?,
    val price: BigDecimal,
    val currency: Currency,
) {
    init {
        require(label.isNotBlank()) { "Le libellé du service est obligatoire" }
        require(price >= BigDecimal.ZERO) { "Le prix du service doit être positif" }
    }
}
