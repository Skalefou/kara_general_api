package com.kara.kara_general_api.application.service.service

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.service.Service
import com.kara.kara_general_api.domain.model.service.ServiceId
import com.kara.kara_general_api.domain.port.input.service.UpdateServiceCommand
import com.kara.kara_general_api.domain.port.input.service.UpdateServiceResult
import com.kara.kara_general_api.domain.port.output.ServiceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpdateServiceServiceTest {
    private val serviceRepository = mockk<ServiceRepository>()
    private val sut = UpdateServiceService(serviceRepository)

    private val id = ServiceId(UUID.randomUUID())
    private val existing =
        Service(
            id = id,
            label = "Ménage fin de soirée",
            description = "Nettoyage complet",
            price = BigDecimal("60.00"),
            currency = Currency.EUR,
        )

    @Test
    fun `should apply only the non-null fields and keep the rest unchanged`() {
        every { serviceRepository.findById(id) } returns existing
        val saved = slot<Service>()
        every { serviceRepository.save(capture(saved)) } answers { saved.captured }

        val result =
            sut.updateService(
                UpdateServiceCommand(
                    id = id,
                    label = "Ménage express",
                    description = null,
                    price = BigDecimal("75.00"),
                    currency = null,
                ),
            )

        val success = assertIs<UpdateServiceResult.Success>(result)
        assertEquals("Ménage express", success.service.label)
        assertEquals("Nettoyage complet", success.service.description)
        assertEquals(BigDecimal("75.00"), success.service.price)
        assertEquals(Currency.EUR, success.service.currency)
        assertEquals(id, success.service.id)
    }

    @Test
    fun `should return NotFound when the service does not exist`() {
        every { serviceRepository.findById(id) } returns null

        val result =
            sut.updateService(
                UpdateServiceCommand(id = id, label = "x", description = null, price = null, currency = null),
            )

        assertEquals(UpdateServiceResult.NotFound, result)
    }
}
