package com.kara.kara_general_api.domain.model.booking

import com.kara.kara_general_api.domain.model.room.Currency
import java.math.BigDecimal

/**
 * Résultat d'un calcul d'estimation de réservation (aucune persistance).
 *
 * - [base] : prix horaire de la salle × nombre de personnes × nombre d'heures (arrondi 2 décimales).
 * - [optionsTotal] : somme des forfaits fixes des options retenues (arrondi 2 décimales).
 * - [totalPrice] : [base] + [optionsTotal].
 * - [pricePerPerson] : [totalPrice] / nombre de personnes (arrondi 2 décimales HALF_UP).
 */
data class BookingEstimate(
    val totalPrice: BigDecimal,
    val pricePerPerson: BigDecimal,
    val currency: Currency,
    val base: BigDecimal,
    val optionsTotal: BigDecimal,
)
