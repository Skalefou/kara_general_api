package com.kara.kara_general_api.infrastructure.adapter.output.messaging.imagejob

import com.kara.kara_general_api.domain.model.image.ImageProcessingJob
import com.kara.kara_general_api.domain.model.image.ImageProcessingTarget
import com.kara.kara_general_api.domain.port.output.ImageProcessingPort
import com.kara.kara_general_api.infrastructure.config.RabbitConfig
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Adaptateur secondaire : publie un [ImageProcessingJob] sur la queue `image-jobs` au format figé du contrat
 * ([ImageJobMessage]). Construit les buckets, le préfixe de clé et le jeu de variantes selon la cible :
 * salle (3 variantes, cible publique) vs profil (2 variantes, cible privée).
 *
 * Le champ `roomId` du contrat porte le `roomId` pour une salle et — faute de champ dédié dans le contrat figé —
 * le `userId` pour une photo de profil ; le worker n'utilise ce champ que comme métadonnée (le travail réel
 * s'appuie sur `source`/`target`), il est donc sans impact fonctionnel.
 */
@Component
@Profile("!test")
class RabbitImageJobPublisher(
    private val rabbitTemplate: RabbitTemplate,
    @Value("\${GCS_BUCKET_PUBLIC}") private val publicBucket: String,
    @Value("\${GCS_BUCKET_PRIVATE}") private val privateBucket: String,
) : ImageProcessingPort {

    override fun enqueue(job: ImageProcessingJob) {
        rabbitTemplate.convertAndSend(RabbitConfig.JOBS_QUEUE, toMessage(job))
    }

    private fun toMessage(job: ImageProcessingJob): ImageJobMessage {
        val targetBucket = if (job.target == ImageProcessingTarget.ROOM) publicBucket else privateBucket
        val keyRoot = if (job.target == ImageProcessingTarget.ROOM) "rooms" else "profiles"
        return ImageJobMessage(
            schemaVersion = SCHEMA_VERSION,
            jobId = job.jobId.toString(),
            roomId = job.ownerId.toString(),
            imageId = job.imageId.toString(),
            source =
                ImageJobSource(
                    bucket = privateBucket,
                    key = job.sourceKey,
                    contentType = job.contentType,
                ),
            target =
                ImageJobTarget(
                    bucket = targetBucket,
                    keyPrefix = "$keyRoot/${job.ownerId}/${job.imageId}",
                ),
            variants = variantsFor(job.target),
            replyTo = RabbitConfig.RESULTS_QUEUE,
            enqueuedAt = Instant.now().toString(),
        )
    }

    private fun variantsFor(target: ImageProcessingTarget): List<ImageJobVariant> =
        when (target) {
            ImageProcessingTarget.ROOM ->
                listOf(
                    ImageJobVariant("thumbnail", 320, 320, "cover", "webp"),
                    ImageJobVariant("detail", 1024, 768, "contain", "webp"),
                    ImageJobVariant("full", 2048, 2048, "inside", "webp"),
                )
            ImageProcessingTarget.PROFILE ->
                listOf(
                    ImageJobVariant("thumbnail", 320, 320, "cover", "webp"),
                    ImageJobVariant("full", 1024, 1024, "inside", "webp"),
                )
        }

    private companion object {
        const val SCHEMA_VERSION = 1
    }
}
