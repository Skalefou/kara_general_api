package com.kara.kara_general_api.application.service.service

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.service.Service
import com.kara.kara_general_api.domain.port.input.service.CreateServiceCommand
import com.kara.kara_general_api.domain.port.output.ServiceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class CreateServiceServiceTest {

    private val serviceRepository = mockk<ServiceRepository>()
    private val sut = CreateServiceService(serviceRepository)

    @Test
    fun `should generate an id and persist the new service`() {
        val saved = slot<Service>()
        every { serviceRepository.save(capture(saved)) } answers { saved.captured }

        val result =
            sut.createService(
                CreateServiceCommand(
                    label = "Ménage fin de soirée",
                    description = "Nettoyage complet",
                    price = BigDecimal("60.00"),
                    currency = Currency.EUR,
                ),
            )

        verify(exactly = 1) { serviceRepository.save(any()) }
        assertEquals("Ménage fin de soirée", result.label)
        assertEquals(BigDecimal("60.00"), result.price)
        assertEquals(Currency.EUR, result.currency)
        assertEquals(saved.captured.id, result.id)
    }
}
