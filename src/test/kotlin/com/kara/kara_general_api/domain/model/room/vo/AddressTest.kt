package com.kara.kara_general_api.domain.model.room.vo

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class AddressTest {

    private fun validAddress(
        street: String = "12 rue de la Paix",
        city: String = "Paris",
        postalCode: String = "75002",
        country: String = "France",
    ) = Address(street = street, city = city, postalCode = postalCode, country = country)

    @Test
    fun `should create address when all fields are valid`() {
        validAddress()
    }

    @Test
    fun `should throw when street is blank`() {
        assertFailsWith<IllegalArgumentException> { validAddress(street = "  ") }
    }

    @Test
    fun `should throw when city is blank`() {
        assertFailsWith<IllegalArgumentException> { validAddress(city = "  ") }
    }

    @Test
    fun `should throw when postal code is blank`() {
        assertFailsWith<IllegalArgumentException> { validAddress(postalCode = "  ") }
    }

    @Test
    fun `should throw when country is blank`() {
        assertFailsWith<IllegalArgumentException> { validAddress(country = "  ") }
    }
}
