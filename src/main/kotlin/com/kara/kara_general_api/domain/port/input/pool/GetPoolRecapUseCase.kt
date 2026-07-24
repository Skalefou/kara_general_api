package com.kara.kara_general_api.domain.port.input.pool

sealed interface GetPoolRecapResult {
    data class Found(val view: PoolRecapView) : GetPoolRecapResult

    data object NotFound : GetPoolRecapResult
}

/** Récapitulatif public d'une cagnotte : lecture sans authentification (le paiement, lui, requiert l'auth). */
interface GetPoolRecapUseCase {
    fun getByGlobalToken(globalToken: String): GetPoolRecapResult

    fun getByShareToken(shareToken: String): GetPoolRecapResult
}
