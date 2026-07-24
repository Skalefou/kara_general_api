package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.UserId

/**
 * Port de sortie modélisant la dépendance de la commande vis-à-vis du moyen de paiement enregistré par le
 * client. L'implémentation réelle (interrogation du moyen de paiement Stripe sauvegardé) est fournie par la
 * branche paiement. Sur cette branche, un adaptateur de repli répond systématiquement `false` afin que la
 * commande signale l'absence de moyen de paiement (PaymentMethodRequired) et propose sa mise en place.
 */
interface PaymentMethodPort {
    /** Vrai si le client [userId] dispose d'un moyen de paiement enregistré et débitable. */
    fun hasRegisteredPaymentMethod(userId: UserId): Boolean
}
