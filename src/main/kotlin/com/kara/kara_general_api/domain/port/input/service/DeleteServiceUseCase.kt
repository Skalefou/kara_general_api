package com.kara.kara_general_api.domain.port.input.service

import com.kara.kara_general_api.domain.model.service.ServiceId

sealed interface DeleteServiceResult {
    data object Success : DeleteServiceResult

    data object NotFound : DeleteServiceResult
}

interface DeleteServiceUseCase {
    fun deleteService(id: ServiceId): DeleteServiceResult
}
