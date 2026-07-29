package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.port.output.PoolLinkBuilder

/** Base de liens utilisée par les doubles de test (miroir de la valeur de production). */
const val TEST_LINK_BASE_URL = "https://link.karapi.fr"

/**
 * Double de test du port [PoolLinkBuilder] : base fixe, mêmes chemins figés (`/join`, `/p`) que
 * l'implémentation de production, sans dépendance à la configuration Spring.
 */
class FakePoolLinkBuilder(
    private val base: String = TEST_LINK_BASE_URL,
) : PoolLinkBuilder {
    override fun globalShareUrl(globalToken: String): String = "$base/join/$globalToken"

    override fun shareUrl(uniqueToken: String): String = "$base/p/$uniqueToken"
}
