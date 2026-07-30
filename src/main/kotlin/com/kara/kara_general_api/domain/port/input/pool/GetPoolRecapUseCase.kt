package com.kara.kara_general_api.domain.port.input.pool

import com.kara.kara_general_api.domain.model.user.UserId

sealed interface GetPoolRecapResult {
    data class Found(
        val view: PoolRecapView,
    ) : GetPoolRecapResult

    data object NotFound : GetPoolRecapResult
}

/** Récapitulatif public d'une cagnotte : lecture sans authentification (le paiement, lui, requiert l'auth). */
interface GetPoolRecapUseCase {
    /**
     * Récapitulatif via le lien global. Lecture **publique** : [callerId] est facultatif.
     *
     * - `null` (invité, ou jeton absent/expiré/invalide) → récapitulatif seul, sans champ `share`.
     * - renseigné → la part dont l'appelant est le **payeur**, et elle seule, est jointe au récapitulatif.
     *
     * Cette dernière lecture existe pour permettre la **reprise d'un paiement interrompu** : quand le
     * PaymentSheet échoue après l'auto-inscription, la part existe déjà en base et la règle « une part par
     * personne » interdit d'en recréer une. Sans son `shareId`, le participant n'aurait aucun moyen de relancer
     * le paiement (`POST /pools/{poolId}/shares/{shareId}/payment`) et resterait bloqué jusqu'à l'échéance.
     *
     * [callerId] provient exclusivement du contexte de sécurité, jamais d'un paramètre de requête, et le
     * filtrage se fait au niveau SQL sur le payeur : la part d'un tiers ne peut donc jamais être exposée.
     */
    fun getByGlobalToken(
        globalToken: String,
        callerId: UserId?,
    ): GetPoolRecapResult

    fun getByShareToken(shareToken: String): GetPoolRecapResult
}
