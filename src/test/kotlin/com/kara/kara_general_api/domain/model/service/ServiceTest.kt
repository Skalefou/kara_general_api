package com.kara.kara_general_api.domain.model.service

import com.kara.kara_general_api.domain.model.room.Currency
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals

class ServiceTest {
    private val id = ServiceId(UUID.randomUUID())

    @Test
    fun `should build a service when label is present and price is positive`() {
        val service =
            Service(
                id = id,
                label = "Ménage fin de soirée",
                description = "Nettoyage complet",
                price = BigDecimal("60.00"),
                currency = Currency.EUR,
            )

        assertEquals("Ménage fin de soirée", service.label)
        assertEquals(BigDecimal("60.00"), service.price)
    }

    @Test
    fun `should build a service when price is zero`() {
        val service =
            Service(
                id = id,
                label = "Accueil offert",
                description = null,
                price = BigDecimal.ZERO,
                currency = Currency.EUR,
            )

        assertEquals(BigDecimal.ZERO, service.price)
    }

    @Test
    fun `should throw when label is blank`() {
        assertThrows<IllegalArgumentException> {
            Service(
                id = id,
                label = "   ",
                description = null,
                price = BigDecimal("60.00"),
                currency = Currency.EUR,
            )
        }
    }

    @Test
    fun `should throw when price is negative`() {
        assertThrows<IllegalArgumentException> {
            Service(
                id = id,
                label = "DJ Set",
                description = null,
                price = BigDecimal("-1.00"),
                currency = Currency.EUR,
            )
        }
    }
}
