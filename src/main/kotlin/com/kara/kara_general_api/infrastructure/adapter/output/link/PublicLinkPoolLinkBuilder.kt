package com.kara.kara_general_api.infrastructure.adapter.output.link

import com.kara.kara_general_api.domain.port.output.PoolLinkBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Implémente [PoolLinkBuilder] à partir de la base de liens publics configurée.
 *
 * Propriété `app.public-link-base-url`, alimentée par la variable d'environnement
 * `APP_PUBLIC_LINK_BASE_URL` :
 * - production : `https://link.karapi.fr`
 * - dev / local : `kara://pool` (schéma personnalisé de l'app mobile, valeur par défaut)
 *
 * Un éventuel `/` final est retiré à la construction, la base est donc toujours normalisée.
 */
@Component
class PublicLinkPoolLinkBuilder(
    @Value("\${app.public-link-base-url:kara://pool}") baseUrl: String,
) : PoolLinkBuilder {
    private val base: String = baseUrl.trimEnd('/')

    override fun globalShareUrl(globalToken: String): String = "$base/join/$globalToken"

    override fun shareUrl(uniqueToken: String): String = "$base/p/$uniqueToken"
}
