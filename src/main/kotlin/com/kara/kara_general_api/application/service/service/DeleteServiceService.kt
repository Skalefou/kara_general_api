package com.kara.kara_general_api.application.service.service

import com.kara.kara_general_api.domain.model.service.ServiceId
import com.kara.kara_general_api.domain.port.input.service.DeleteServiceResult
import com.kara.kara_general_api.domain.port.input.service.DeleteServiceUseCase
import com.kara.kara_general_api.domain.port.output.ServiceRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service as SpringService

@SpringService
class DeleteServiceService(
    private val serviceRepository: ServiceRepository,
) : DeleteServiceUseCase {
    @Transactional
    override fun deleteService(id: ServiceId): DeleteServiceResult {
        val deleted = serviceRepository.deleteById(id)
        return if (deleted) DeleteServiceResult.Success else DeleteServiceResult.NotFound
    }
}
