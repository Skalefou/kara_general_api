package com.kara.kara_general_api.infrastructure.adapter.input.rest.service

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.service.Service
import com.kara.kara_general_api.domain.model.service.ServiceId
import com.kara.kara_general_api.domain.port.input.service.CreateServiceUseCase
import com.kara.kara_general_api.domain.port.input.service.DeleteServiceResult
import com.kara.kara_general_api.domain.port.input.service.DeleteServiceUseCase
import com.kara.kara_general_api.domain.port.input.service.ListServicesUseCase
import com.kara.kara_general_api.domain.port.input.service.UpdateServiceResult
import com.kara.kara_general_api.domain.port.input.service.UpdateServiceUseCase
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.util.UUID

private const val SERVICE_ID = "22222222-2222-2222-2222-222222222221"
private const val REQUEST_BODY =
    """{"label": "Ménage fin de soirée", "description": "Nettoyage complet", "price": 60.00, "currency": "EUR"}"""

@WebMvcTest(ServiceController::class)
@Import(SecurityConfig::class)
class ServiceControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var createServiceUseCase: CreateServiceUseCase

    @MockkBean
    private lateinit var listServicesUseCase: ListServicesUseCase

    @MockkBean
    private lateinit var updateServiceUseCase: UpdateServiceUseCase

    @MockkBean
    private lateinit var deleteServiceUseCase: DeleteServiceUseCase

    private val service =
        Service(
            id = ServiceId(UUID.fromString(SERVICE_ID)),
            label = "Ménage fin de soirée",
            description = "Nettoyage complet",
            price = BigDecimal("60.00"),
            currency = Currency.EUR,
        )

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 201 when admin creates a service`() {
        every { createServiceUseCase.createService(any()) } returns service

        mockMvc
            .perform(
                post("/api/v1/services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST_BODY),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(SERVICE_ID))
            .andExpect(jsonPath("$.label").value("Ménage fin de soirée"))
            .andExpect(jsonPath("$.price").value(60.00))
            .andExpect(jsonPath("$.currency").value("EUR"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 400 when the create body fails bean validation`() {
        val invalidBody = """{"label": "  ", "description": null, "price": -5, "currency": "EUR"}"""

        mockMvc
            .perform(
                post("/api/v1/services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))

        verify(exactly = 0) { createServiceUseCase.createService(any()) }
    }

    @Test
    @WithMockUser(roles = ["CLIENT"])
    fun `should return 403 when non-admin creates a service`() {
        mockMvc
            .perform(
                post("/api/v1/services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST_BODY),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should return 401 when unauthenticated creates a service`() {
        mockMvc
            .perform(
                post("/api/v1/services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST_BODY),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 200 with the catalog when admin lists services`() {
        every { listServicesUseCase.listServices() } returns listOf(service)

        mockMvc
            .perform(get("/api/v1/services"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].label").value("Ménage fin de soirée"))
    }

    @Test
    @WithMockUser(roles = ["CLIENT"])
    fun `should return 403 when non-admin lists services`() {
        mockMvc
            .perform(get("/api/v1/services"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 200 when admin updates a service`() {
        every { updateServiceUseCase.updateService(any()) } returns
            UpdateServiceResult.Success(service.copy(label = "Ménage express"))

        mockMvc
            .perform(
                patch("/api/v1/services/$SERVICE_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"label": "Ménage express"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.label").value("Ménage express"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 400 when the update body fails bean validation`() {
        mockMvc
            .perform(
                patch("/api/v1/services/$SERVICE_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"price": -5}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))

        verify(exactly = 0) { updateServiceUseCase.updateService(any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 404 when admin updates an unknown service`() {
        every { updateServiceUseCase.updateService(any()) } returns UpdateServiceResult.NotFound

        mockMvc
            .perform(
                patch("/api/v1/services/$SERVICE_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"label": "x"}"""),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"))
    }

    @Test
    @WithMockUser(roles = ["CLIENT"])
    fun `should return 403 when non-admin updates a service`() {
        mockMvc
            .perform(
                patch("/api/v1/services/$SERVICE_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"label": "x"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should return 401 when unauthenticated updates a service`() {
        mockMvc
            .perform(
                patch("/api/v1/services/$SERVICE_ID")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"label": "x"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 204 when admin deletes a service`() {
        every { deleteServiceUseCase.deleteService(ServiceId(UUID.fromString(SERVICE_ID))) } returns
            DeleteServiceResult.Success

        mockMvc
            .perform(delete("/api/v1/services/$SERVICE_ID"))
            .andExpect(status().isNoContent)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should return 404 when admin deletes an unknown service`() {
        every { deleteServiceUseCase.deleteService(ServiceId(UUID.fromString(SERVICE_ID))) } returns
            DeleteServiceResult.NotFound

        mockMvc
            .perform(delete("/api/v1/services/$SERVICE_ID"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"))
    }

    @Test
    @WithMockUser(roles = ["CLIENT"])
    fun `should return 403 when non-admin deletes a service`() {
        mockMvc
            .perform(delete("/api/v1/services/$SERVICE_ID"))
            .andExpect(status().isForbidden)
    }
}
