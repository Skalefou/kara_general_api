package com.kara.kara_general_api.domain.port.input.service

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.service.Service
import java.math.BigDecimal

data class CreateServiceCommand(
    val label: String,
    val description: String?,
    val price: BigDecimal,
    val currency: Currency,
)

interface CreateServiceUseCase {
    fun createService(command: CreateServiceCommand): Service
}
