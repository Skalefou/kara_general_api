package com.kara.kara_general_api.infrastructure.adapter.input.rest.service

import com.kara.kara_general_api.domain.model.service.ServiceId
import com.kara.kara_general_api.domain.port.input.service.CreateServiceCommand
import com.kara.kara_general_api.domain.port.input.service.CreateServiceUseCase
import com.kara.kara_general_api.domain.port.input.service.DeleteServiceResult
import com.kara.kara_general_api.domain.port.input.service.DeleteServiceUseCase
import com.kara.kara_general_api.domain.port.input.service.ListServicesUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.service.dto.CreateServiceRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.service.dto.ServiceResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/services")
class ServiceController(
    private val createServiceUseCase: CreateServiceUseCase,
    private val listServicesUseCase: ListServicesUseCase,
    private val deleteServiceUseCase: DeleteServiceUseCase,
) : ServiceApi {

    override fun createService(request: CreateServiceRequest): ResponseEntity<Any> {
        val service =
            createServiceUseCase.createService(
                CreateServiceCommand(
                    label = request.label,
                    description = request.description,
                    price = request.price,
                    currency = request.currency,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(ServiceResponse.from(service))
    }

    override fun listServices(): ResponseEntity<Any> =
        ResponseEntity.ok(listServicesUseCase.listServices().map { ServiceResponse.from(it) })

    override fun deleteService(id: UUID): ResponseEntity<Any> =
        when (deleteServiceUseCase.deleteService(ServiceId(id))) {
            DeleteServiceResult.Success -> ResponseEntity.noContent().build()
            DeleteServiceResult.NotFound -> serviceNotFound()
        }

    private fun serviceNotFound(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Aucun service ne correspond à cet identifiant.",
            ).apply {
                title = "Service introuvable"
                setProperty("code", "SERVICE_NOT_FOUND")
            },
        )
}
