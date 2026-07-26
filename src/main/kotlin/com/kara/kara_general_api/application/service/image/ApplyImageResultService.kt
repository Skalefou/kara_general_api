package com.kara.kara_general_api.application.service.image

import com.kara.kara_general_api.domain.model.image.ImageProcessingTarget
import com.kara.kara_general_api.domain.model.room.RoomImageVariant
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.image.ApplyImageResultCommand
import com.kara.kara_general_api.domain.port.input.image.ApplyImageResultUseCase
import com.kara.kara_general_api.domain.port.output.ImageJobCorrelationRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DEFAULT_ERROR_CODE = "INTERNAL"

/**
 * Applique le résultat d'un job d'image renvoyé par le worker. Idempotent (livraison at-least-once) :
 * - jobId inconnu (corrélation absente ou déjà purgée) → ignoré silencieusement ;
 * - un rejeu écrase le même statut / les mêmes variantes (clés déterministes).
 */
@Service
class ApplyImageResultService(
    private val correlationRepository: ImageJobCorrelationRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
) : ApplyImageResultUseCase {
    @Transactional
    override fun apply(command: ApplyImageResultCommand) {
        val correlation = correlationRepository.findByJobId(command.jobId) ?: return
        when (correlation.target) {
            ImageProcessingTarget.ROOM -> applyRoom(correlation.imageId, command)
            ImageProcessingTarget.PROFILE -> applyProfile(UserId(correlation.ownerId), command)
        }
    }

    private fun applyRoom(
        imageId: java.util.UUID,
        command: ApplyImageResultCommand,
    ) {
        if (command.success) {
            val variants =
                command.variants.map {
                    RoomImageVariant(
                        name = it.name,
                        objectKey = it.objectKey,
                        width = it.width,
                        height = it.height,
                        sizeBytes = it.sizeBytes,
                        contentType = it.contentType,
                    )
                }
            roomRepository.markImageReady(imageId, variants)
        } else {
            roomRepository.markImageFailed(imageId, command.errorCode ?: DEFAULT_ERROR_CODE)
        }
    }

    private fun applyProfile(
        userId: UserId,
        command: ApplyImageResultCommand,
    ) {
        if (!command.success) {
            userRepository.markPhotoFailed(userId)
            return
        }
        val thumbnailKey = command.variants.firstOrNull { it.name == "thumbnail" }?.objectKey
        val fullKey = command.variants.firstOrNull { it.name == "full" }?.objectKey
        if (thumbnailKey != null && fullKey != null) {
            userRepository.markPhotoReady(userId, thumbnailKey, fullKey)
        } else {
            // Résultat "ok" mais variantes attendues absentes : on ne peut pas afficher la photo → FAILED.
            userRepository.markPhotoFailed(userId)
        }
    }
}
