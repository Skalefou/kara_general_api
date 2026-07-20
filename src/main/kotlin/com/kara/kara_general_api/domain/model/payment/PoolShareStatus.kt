package com.kara.kara_general_api.domain.model.payment

/**
 * Cycle de vie d'une part (cagnotte). PENDING → AUTHORIZED (fonds bloqués via PaymentIntent à capture
 * manuelle) → CAPTURED (prélèvement effectif quand toute la cagnotte est complète), ou CANCELLED
 * (autorisation annulée : aucun prélèvement).
 */
enum class PoolShareStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    CANCELLED,
}
