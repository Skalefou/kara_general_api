package com.kara.kara_general_api.infrastructure.adapter.output.messaging.imagejob

/**
 * Miroir EXACT du contrat de message `image-jobs` (API → worker Rust), défini et figé dans
 * `kara-image-worker/CLAUDE.md` §"Contrat des messages". Sérialisé en JSON camelCase avec `schemaVersion = 1`.
 * Aucun octet d'image ne transite : uniquement des clés d'objets. La construction concrète (buckets,
 * variantes salle vs profil) est faite par [RabbitImageJobPublisher].
 */
data class ImageJobMessage(
    val schemaVersion: Int,
    val jobId: String,
    val roomId: String,
    val imageId: String,
    val source: ImageJobSource,
    val target: ImageJobTarget,
    val variants: List<ImageJobVariant>,
    val replyTo: String,
    val enqueuedAt: String,
)

data class ImageJobSource(
    val bucket: String,
    val key: String,
    val contentType: String,
)

data class ImageJobTarget(
    val bucket: String,
    val keyPrefix: String,
)

data class ImageJobVariant(
    val name: String,
    val width: Int,
    val height: Int,
    val fit: String,
    val format: String,
)
