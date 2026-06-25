package com.kara.kara_general_api.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Configuration
class JwtKeyConfig {

    @Bean
    fun jwtPrivateKey(
        @Value("\${JWT_PRIVATE_KEY}") base64PrivateKey: String,
    ): RSAPrivateKey {
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64PrivateKey))
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec) as RSAPrivateKey
    }

    @Bean
    fun jwtPublicKey(
        @Value("\${JWT_PUBLIC_KEY}") base64PublicKey: String,
    ): RSAPublicKey {
        val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey))
        return KeyFactory.getInstance("RSA").generatePublic(keySpec) as RSAPublicKey
    }
}
