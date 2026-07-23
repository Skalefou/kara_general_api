package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.image.ImageProcessingJob

/**
 * Port secondaire : délègue le redimensionnement d'une image à un worker externe **de façon asynchrone**
 * (aucun couplage synchrone). L'adaptateur publie le job sur la queue `image-jobs` ; le résultat revient
 * plus tard sur `image-results`. La méthode ne retourne rien : le succès de la publication n'implique pas
 * la fin du traitement.
 */
interface ImageProcessingPort {
    fun enqueue(job: ImageProcessingJob)
}
