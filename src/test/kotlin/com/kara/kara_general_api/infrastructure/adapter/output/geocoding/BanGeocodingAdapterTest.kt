package com.kara.kara_general_api.infrastructure.adapter.output.geocoding

import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.output.GeocodingException
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BanGeocodingAdapterTest {
    private val address =
        Address(street = "8 boulevard du Port", city = "Amiens", postalCode = "80000", country = "France")

    private fun adapterWith(configure: (MockRestServiceServer) -> Unit): BanGeocodingAdapter {
        val builder = RestClient.builder().baseUrl("http://ban.local")
        val server = MockRestServiceServer.bindTo(builder).build()
        configure(server)
        return BanGeocodingAdapter(builder.build())
    }

    @Test
    fun `should map GeoJSON lon-lat order to Coordinates lat-lng`() {
        val body = """{"features":[{"geometry":{"type":"Point","coordinates":[2.3522,48.8566]}}]}"""
        val adapter =
            adapterWith { server ->
                server
                    .expect(requestTo(containsString("/search/")))
                    .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))
            }

        val coordinates = adapter.geocode(address)

        assertEquals(48.8566, coordinates?.latitude)
        assertEquals(2.3522, coordinates?.longitude)
    }

    @Test
    fun `should return null when no feature matches the address`() {
        val adapter =
            adapterWith { server ->
                server
                    .expect(requestTo(containsString("/search/")))
                    .andRespond(withSuccess("""{"features":[]}""", MediaType.APPLICATION_JSON))
            }

        assertNull(adapter.geocode(address))
    }

    @Test
    fun `should throw GeocodingException when the service errors`() {
        val adapter =
            adapterWith { server ->
                server
                    .expect(requestTo(containsString("/search/")))
                    .andRespond(withServerError())
            }

        assertThrows<GeocodingException> { adapter.geocode(address) }
    }
}
