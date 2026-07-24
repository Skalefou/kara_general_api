package com.kara.kara_general_api.domain.model.booking

/**
 * Mode de règlement choisi à la création d'une réservation.
 *
 * - [PAY_ALL] : le client paie la totalité (PaymentIntent unique). Fenêtre de paiement courte (15 min) ;
 *   passé ce délai, la réservation PENDING est annulée par le planificateur.
 * - [SHARED_POT] : cagnotte partagée. La réservation n'est **pas** soumise à la fenêtre de 15 min : c'est
 *   le délai de la cagnotte qui gouverne son annulation éventuelle.
 */
enum class PaymentMode {
    PAY_ALL,
    SHARED_POT,
}
