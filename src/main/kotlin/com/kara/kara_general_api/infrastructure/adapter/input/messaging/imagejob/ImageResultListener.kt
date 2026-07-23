package com.kara.kara_general_api.infrastructure.adapter.input.messaging.imagejob

import com.kara.kara_general_api.domain.port.input.image.AppliedImageVariant
import com.kara.kara_general_api.domain.port.input.image.ApplyImageResultCommand
import com.kara.kara_general_api.domain.port.input.image.ApplyImageResultUseCase
import com.kara.kara_general_api.infrastructure.adapter.output.messaging.imagejob.ImageErrorCode
import com.kara.kara_general_api.infrastructure.adapter.output.messaging.imagejob.ImageResultMessage
import com.kara.kara_general_api.infrastructure.config.RabbitConfig
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Adaptateur primaire : consomme les résultats du worker sur `image-results` et délègue au use case
 * d'application, qui persiste idempotemment. Le message est désérialisé par le [org.springframework.amqp.support.converter.JacksonJsonMessageConverter]
 * configuré dans [RabbitConfig] (miroir du contrat figé).
 */
@Component
@Profile("!test")
class ImageResultListener(
    private val applyImageResultUseCase: ApplyImageResultUseCase,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [RabbitConfig.RESULTS_QUEUE])
    fun onResult(message: ImageResultMessage) {
        val jobId =
            runCatching { UUID.fromString(message.jobId) }
                .getOrElse {
                    logger.warn("Discarding image result with malformed jobId: {}", message.jobId)
                    return
                }

        val command =
            if (message.isOk()) {
                ApplyImageResultCommand(
                    jobId = jobId,
                    success = true,
                    variants =
                        message.variants.orEmpty().map {
                            AppliedImageVariant(
                                name = it.name,
                                objectKey = it.key,
                                width = it.width,
                                height = it.height,
                                sizeBytes = it.sizeBytes,
                                contentType = it.contentType,
                            )
                        },
                )
            } else {
                ApplyImageResultCommand(
                    jobId = jobId,
                    success = false,
                    errorCode = ImageErrorCode.fromWire(message.error?.code).name,
                )
            }

        applyImageResultUseCase.apply(command)
    }
}
