package com.kara.kara_general_api.infrastructure.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.ByteArrayInputStream
import java.util.Base64

/**
 * Client Google Cloud Storage. Réutilise le compte de service Firebase (même projet GCP) :
 * la clé privée du service account permet aussi la signature des URL V4 du bucket privé.
 */
@Configuration
@Profile("!test")
class GcsConfig(
    @Value("\${FIREBASE_CREDENTIALS_BASE64}") private val credentialsBase64: String,
) {
    @Bean
    fun storage(): Storage {
        val credentialsJson = Base64.getDecoder().decode(credentialsBase64)
        val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(credentialsJson))
        return StorageOptions
            .newBuilder()
            .setCredentials(credentials)
            .build()
            .service
    }
}
