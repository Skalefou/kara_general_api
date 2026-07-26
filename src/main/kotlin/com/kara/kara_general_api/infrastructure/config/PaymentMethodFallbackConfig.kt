package com.kara.kara_general_api.infrastructure.config

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.PaymentMethodPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Adaptateur de repli pour [PaymentMethodPort].
 *
 * Le véritable adaptateur (interrogation du moyen de paiement Stripe enregistré par le client) est fourni par
 * la branche paiement. Tant qu'il n'est pas sur le classpath, ce repli — activé uniquement en l'absence d'un
 * autre bean [PaymentMethodPort] — répond systématiquement `false` : la commande signale alors l'absence de
 * moyen de paiement (PaymentMethodRequired) pour en proposer la mise en place. Cela permet à l'application de
 * démarrer sur cette branche sans dépendre de la mécanique de paiement.
 */
@Configuration
class PaymentMethodFallbackConfig {
    @Bean
    @ConditionalOnMissingBean(PaymentMethodPort::class)
    fun fallbackPaymentMethodPort(): PaymentMethodPort =
        object : PaymentMethodPort {
            override fun hasRegisteredPaymentMethod(userId: UserId): Boolean = false
        }
}
