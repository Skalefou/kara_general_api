package com.kara.kara_general_api.infrastructure.adapter.output.geocoding

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.room.vo.Coordinates
import com.kara.kara_general_api.domain.port.output.GeocodingException
import com.kara.kara_general_api.domain.port.output.GeocodingPort
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body

/**
 * Adaptateur de géocodage sur la Base Adresse Nationale (api-adresse.data.gouv.fr).
 * Réponse GeoJSON : features[0].geometry.coordinates = [longitude, latitude].
 */
@Component
class BanGeocodingAdapter(
    private val geocodingRestClient: RestClient,
) : GeocodingPort {
    override fun geocode(address: Address): Coordinates? {
        val query = "${address.street} ${address.postalCode} ${address.city}"
        val response =
            try {
                geocodingRestClient
                    .get()
                    .uri { builder ->
                        builder
                            .path("/search/")
                            .queryParam("q", query)
                            .queryParam("limit", 1)
                            .build()
                    }.retrieve()
                    .body<BanResponse>()
            } catch (ex: RestClientException) {
                throw GeocodingException("Le service de géocodage est indisponible", ex)
            }

        val coordinates =
            response
                ?.features
                ?.firstOrNull()
                ?.geometry
                ?.coordinates ?: return null
        if (coordinates.size < 2) return null
        return Coordinates(latitude = coordinates[1], longitude = coordinates[0])
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class BanResponse(
    val features: List<BanFeature> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class BanFeature(
    val geometry: BanGeometry? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class BanGeometry(
    val coordinates: List<Double> = emptyList(),
)
