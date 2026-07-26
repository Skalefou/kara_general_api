package com.kara.kara_general_api.infrastructure.adapter.output.security

import com.kara.kara_general_api.domain.port.output.LinkTokenGenerator
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64

private const val TOKEN_BYTES = 32

/** Génère des tokens de lien opaques (256 bits, encodés en Base64 URL-safe sans padding). */
@Component
class SecureRandomLinkTokenGenerator : LinkTokenGenerator {
    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    override fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }
}
