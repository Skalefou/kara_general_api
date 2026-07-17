package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.service.Service
import com.kara.kara_general_api.domain.model.service.ServiceId

interface ServiceRepository {
    fun save(service: Service): Service

    /** Catalogue global complet, ordonné par libellé. */
    fun findAll(): List<Service>

    fun findById(id: ServiceId): Service?

    /** Supprime un service du catalogue. Retourne true si une ligne a été supprimée. */
    fun deleteById(id: ServiceId): Boolean

    fun existsById(id: ServiceId): Boolean
}
