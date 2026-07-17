package com.kara.kara_general_api.domain.port.input.service

import com.kara.kara_general_api.domain.model.service.Service

interface ListServicesUseCase {
    /** Catalogue global des services, ordonné par libellé. */
    fun listServices(): List<Service>
}
