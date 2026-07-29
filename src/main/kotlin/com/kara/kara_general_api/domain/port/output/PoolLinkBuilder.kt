package com.kara.kara_general_api.domain.port.output

/**
 * Construit les liens de partage publics d'une cagnotte (deep links) à partir des tokens opaques
 * produits par [LinkTokenGenerator]. L'API est la **seule** source de vérité de ces URLs : les fronts
 * ne concatènent jamais de domaine eux-mêmes.
 *
 * Les deux chemins sont **figés** — les applications mobiles déclarent des intent-filters Android,
 * un entitlement iOS et un `apple-app-site-association` sur les préfixes `/join` et `/p` :
 * - lien global d'une cagnotte : `{base}/join/{globalToken}`
 * - lien unique d'une part     : `{base}/p/{uniqueToken}`
 */
interface PoolLinkBuilder {
    /** Lien global de la cagnotte : partageable à tous les participants. */
    fun globalShareUrl(globalToken: String): String

    /** Lien unique d'une part : destiné au seul participant invité. */
    fun shareUrl(uniqueToken: String): String
}
