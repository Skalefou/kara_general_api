package com.kara.kara_general_api.application.service.service

import com.kara.kara_general_api.domain.model.service.Service
import com.kara.kara_general_api.domain.port.input.service.ListServicesUseCase
import com.kara.kara_general_api.domain.port.output.ServiceRepository
import org.springframework.stereotype.Service as SpringService

@SpringService
class ListServicesService(
    private val serviceRepository: ServiceRepository,
) : ListServicesUseCase {
    override fun listServices(): List<Service> = serviceRepository.findAll()
}
