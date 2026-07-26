package com.kara.kara_general_api.application.service.service

import com.kara.kara_general_api.domain.model.service.ServiceId
import com.kara.kara_general_api.domain.port.input.service.DeleteServiceResult
import com.kara.kara_general_api.domain.port.output.ServiceRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class DeleteServiceServiceTest {
    private val serviceRepository = mockk<ServiceRepository>()
    private val sut = DeleteServiceService(serviceRepository)

    private val id = ServiceId(UUID.randomUUID())

    @Test
    fun `should return Success when the service was deleted`() {
        every { serviceRepository.deleteById(id) } returns true

        assertEquals(DeleteServiceResult.Success, sut.deleteService(id))
    }

    @Test
    fun `should return NotFound when no service was deleted`() {
        every { serviceRepository.deleteById(id) } returns false

        assertEquals(DeleteServiceResult.NotFound, sut.deleteService(id))
    }
}
