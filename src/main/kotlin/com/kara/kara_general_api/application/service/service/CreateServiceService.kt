package com.kara.kara_general_api.application.service.service

import com.kara.kara_general_api.domain.model.service.Service
import com.kara.kara_general_api.domain.model.service.ServiceId
import com.kara.kara_general_api.domain.port.input.service.CreateServiceCommand
import com.kara.kara_general_api.domain.port.input.service.CreateServiceUseCase
import com.kara.kara_general_api.domain.port.output.ServiceRepository
import org.springframework.stereotype.Service as SpringService

@SpringService
class CreateServiceService(
    private val serviceRepository: ServiceRepository,
) : CreateServiceUseCase {
    override fun createService(command: CreateServiceCommand): Service {
        val service =
            Service(
                id = ServiceId.generate(),
                label = command.label,
                description = command.description,
                price = command.price,
                currency = command.currency,
            )
        return serviceRepository.save(service)
    }
}
