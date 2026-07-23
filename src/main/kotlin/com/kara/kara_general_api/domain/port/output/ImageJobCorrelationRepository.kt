package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.image.ImageJobCorrelation
import java.util.UUID

/** Port secondaire de persistance de la corrélation jobId → entité (salle/profil) à mettre à jour au retour. */
interface ImageJobCorrelationRepository {
    fun save(correlation: ImageJobCorrelation)

    fun findByJobId(jobId: UUID): ImageJobCorrelation?
}
