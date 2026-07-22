package com.kara.kara_general_api.domain.model.payment

/**
 * Cycle de vie d'une cagnotte (règlement Stripe en autorisation à capture manuelle) :
 * OPEN → (toutes les parts autorisées) AUTHORIZED_COMPLETE → (captures effectuées) SETTLED,
 * ou OPEN → CANCELLED / EXPIRED (délai échu : toutes les autorisations sont annulées, zéro prélèvement).
 */
enum class PoolStatus {
    /** Cagnotte ouverte : les participants autorisent leur part (aucun prélèvement encore). */
    OPEN,

    /** État transitoire : toutes les parts sont autorisées, la capture globale est imminente. */
    AUTHORIZED_COMPLETE,

    /** Toutes les parts ont été capturées : la réservation est confirmée. */
    SETTLED,

    /** Cagnotte annulée avant échéance (aucun prélèvement). */
    CANCELLED,

    /** Délai de la cagnotte échu alors qu'elle était incomplète : autorisations annulées, zéro prélèvement. */
    EXPIRED,
}
