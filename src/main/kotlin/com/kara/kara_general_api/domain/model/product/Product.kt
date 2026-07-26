package com.kara.kara_general_api.domain.model.product

import com.kara.kara_general_api.domain.model.room.Currency
import java.math.BigDecimal

/**
 * Produit consommable du catalogue générique. Ce catalogue est indépendant de toute salle : il sert
 * de liste de référence pour la gestion de stock et la consommation pendant une réservation. Le prix
 * est un tarif unitaire fixe exprimé dans une devise.
 */
data class Product(
    val id: ProductId,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val currency: Currency,
) {
    init {
        require(name.isNotBlank()) { "Le nom du produit est obligatoire" }
        require(price >= BigDecimal.ZERO) { "Le prix du produit doit être positif" }
    }
}
