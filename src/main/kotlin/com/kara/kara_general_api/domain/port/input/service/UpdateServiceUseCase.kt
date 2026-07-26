package com.kara.kara_general_api.domain.port.input.service

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.service.Service
import com.kara.kara_general_api.domain.model.service.ServiceId
import java.math.BigDecimal

/**
 * Mise à jour partielle d'un service du catalogue global : chaque champ non-null remplace la valeur
 * existante ; un champ null laisse la valeur inchangée.
 */
data class UpdateServiceCommand(
    val id: ServiceId,
    val label: String?,
    val description: String?,
    val price: BigDecimal?,
    val currency: Currency?,
)

sealed interface UpdateServiceResult {
    data class Success(
        val service: Service,
    ) : UpdateServiceResult

    data object NotFound : UpdateServiceResult
}

interface UpdateServiceUseCase {
    fun updateService(command: UpdateServiceCommand): UpdateServiceResult
}
