package com.kara.kara_general_api.domain.model.order

/**
 * Statut d'une commande passée pendant une réservation active.
 *
 * Volontairement minimal pour ce périmètre : la commande est simplement enregistrée (PLACED) et le stock
 * décrémenté. Le débit/crédit du moyen de paiement (capture, remboursement) est géré par la branche
 * paiement, hors de cet agrégat ; l'énumération reste extensible pour accueillir d'éventuels états ultérieurs
 * (ex. PREPARING, DELIVERED) sans impacter la logique de commande.
 */
enum class OrderStatus {
    PLACED,
}
