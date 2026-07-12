package com.kara.kara_general_api.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

/**
 * Client HTTP du service de géocodage (Base Adresse Nationale — api-adresse.data.gouv.fr).
 */
@Configuration
class GeocodingConfig {

    @Bean
    fun geocodingRestClient(
        @Value("\${GEOCODING_BASE_URL}") baseUrl: String,
    ): RestClient = RestClient.builder().baseUrl(baseUrl).build()
}
