package com.kara.kara_general_api.application.service.service

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.service.Service
import com.kara.kara_general_api.domain.model.service.ServiceId
import com.kara.kara_general_api.domain.port.output.ServiceRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals

class ListServicesServiceTest {

    private val serviceRepository = mockk<ServiceRepository>()
    private val sut = ListServicesService(serviceRepository)

    @Test
    fun `should return the global catalog from the repository`() {
        val services =
            listOf(
                Service(ServiceId(UUID.randomUUID()), "Agent de sécurité", null, BigDecimal("25.00"), Currency.EUR),
                Service(ServiceId(UUID.randomUUID()), "DJ Set (4h)", null, BigDecimal("300.00"), Currency.EUR),
            )
        every { serviceRepository.findAll() } returns services

        assertEquals(services, sut.listServices())
    }
}
