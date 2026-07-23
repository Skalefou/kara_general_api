package com.kara.kara_general_api.domain.model.image

import java.util.UUID

/** Cible d'un traitement d'image asynchrone : une salle (variantes publiques) ou une photo de profil (privées). */
enum class ImageProcessingTarget {
    ROOM,
    PROFILE,
}

/**
 * Demande de traitement d'image, indépendante du transport (queue) et du format du message de contrat.
 * L'adaptateur secondaire ([com.kara.kara_general_api.domain.port.output.ImageProcessingPort]) traduit ce
 * modèle vers le message figé publié sur `image-jobs` (buckets, préfixe de clé, variantes salle vs profil).
 *
 * @param jobId identifiant de corrélation, repris tel quel dans le résultat.
 * @param ownerId salle (ROOM) ou utilisateur (PROFILE) propriétaire de l'image.
 * @param imageId identité de l'image, utilisée dans le préfixe de clé des variantes.
 * @param sourceKey clé de l'original dans le bucket privé.
 * @param contentType type MIME de l'original.
 */
data class ImageProcessingJob(
    val jobId: UUID,
    val target: ImageProcessingTarget,
    val ownerId: UUID,
    val imageId: UUID,
    val sourceKey: String,
    val contentType: String,
)
