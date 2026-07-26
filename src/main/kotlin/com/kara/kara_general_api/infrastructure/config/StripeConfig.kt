package com.kara.kara_general_api.infrastructure.config

import com.stripe.Stripe
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Configuration Stripe. La clé secrète est appliquée globalement au SDK Stripe (API statique). Désactivé
 * sur le profil "test" : les tests mockent le port [com.kara.kara_general_api.domain.port.output.PaymentGateway].
 *
 * Clés d'environnement (préfixe `kara.stripe.*`) : STRIPE_SECRET_KEY, STRIPE_PUBLISHABLE_KEY,
 * STRIPE_WEBHOOK_SECRET. La clé secrète et le secret de webhook ne sont jamais logués.
 */
@Configuration
@Profile("!test")
class StripeConfig(
    @Value("\${kara.stripe.secret-key}") private val secretKey: String,
) {
    @PostConstruct
    fun init() {
        Stripe.apiKey = secretKey
    }
}
