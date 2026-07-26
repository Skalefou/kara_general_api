package com.kara.kara_general_api.application.service.service

import com.kara.kara_general_api.domain.port.input.service.UpdateServiceCommand
import com.kara.kara_general_api.domain.port.input.service.UpdateServiceResult
import com.kara.kara_general_api.domain.port.input.service.UpdateServiceUseCase
import com.kara.kara_general_api.domain.port.output.ServiceRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service as SpringService

@SpringService
class UpdateServiceService(
    private val serviceRepository: ServiceRepository,
) : UpdateServiceUseCase {
    @Transactional
    override fun updateService(command: UpdateServiceCommand): UpdateServiceResult {
        val existing = serviceRepository.findById(command.id) ?: return UpdateServiceResult.NotFound
        val updated =
            existing.copy(
                label = command.label ?: existing.label,
                description = if (command.description != null) command.description else existing.description,
                price = command.price ?: existing.price,
                currency = command.currency ?: existing.currency,
            )
        return UpdateServiceResult.Success(serviceRepository.save(updated))
    }
}
