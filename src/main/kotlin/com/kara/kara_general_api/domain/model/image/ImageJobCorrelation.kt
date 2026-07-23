package com.kara.kara_general_api.domain.model.image

import java.util.UUID

/**
 * Corrélation persistée entre un `jobId` (échangé avec le worker) et l'entité à mettre à jour au retour
 * du résultat. Le `jobId` du contrat étant un simple UUID opaque, on ne peut pas y encoder le type : on
 * maintient donc cette table de corrélation, écrite dans la même transaction que l'insertion de l'image.
 * Au retour sur `image-results`, elle indique s'il s'agit d'une salle ou d'un profil, et de quelle entité.
 */
data class ImageJobCorrelation(
    val jobId: UUID,
    val target: ImageProcessingTarget,
    val ownerId: UUID,
    val imageId: UUID,
)
