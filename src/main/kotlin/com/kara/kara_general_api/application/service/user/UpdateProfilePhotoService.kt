package com.kara.kara_general_api.application.service.user

import com.kara.kara_general_api.application.service.image.ImageUploadPolicy
import com.kara.kara_general_api.domain.model.image.ImageJobCorrelation
import com.kara.kara_general_api.domain.model.image.ImageProcessingJob
import com.kara.kara_general_api.domain.model.image.ImageProcessingTarget
import com.kara.kara_general_api.domain.port.input.user.UpdateProfilePhotoCommand
import com.kara.kara_general_api.domain.port.input.user.UpdateProfilePhotoResult
import com.kara.kara_general_api.domain.port.input.user.UpdateProfilePhotoUseCase
import com.kara.kara_general_api.domain.port.output.ImageJobCorrelationRepository
import com.kara.kara_general_api.domain.port.output.ImageProcessingPort
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.ImageVisibility
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Flux de mise à jour de la photo de profil **asynchrone** : original téléversé dans le bucket privé,
 * bascule en PROCESSING, corrélation enregistrée, job publié (2 variantes privées). Les objets de l'ancienne
 * photo (original + variantes) sont supprimés. La validation ([ImageUploadPolicy]) reste en amont de l'enqueue.
 */
@Service
class UpdateProfilePhotoService(
    private val userRepository: UserRepository,
    private val imageStorage: ImageStoragePort,
    private val imageProcessing: ImageProcessingPort,
    private val correlationRepository: ImageJobCorrelationRepository,
) : UpdateProfilePhotoUseCase {
    @Transactional
    override fun updatePhoto(command: UpdateProfilePhotoCommand): UpdateProfilePhotoResult {
        val contentType = command.contentType
        if (!ImageUploadPolicy.isAllowedType(contentType)) return UpdateProfilePhotoResult.InvalidImageType
        if (!ImageUploadPolicy.isWithinSize(command.bytes.size)) return UpdateProfilePhotoResult.ImageTooLarge

        val user = userRepository.findById(command.userId) ?: return UpdateProfilePhotoResult.UserNotFound

        val imageId = UUID.randomUUID()
        val jobId = UUID.randomUUID()
        val extension = ImageUploadPolicy.extensionFor(contentType!!)
        val originalKey = "profiles/${user.id.value}/originals/$imageId.$extension"

        imageStorage.upload(ImageVisibility.PRIVATE, originalKey, command.bytes, contentType)
        userRepository.markPhotoProcessing(user.id, originalKey)
        correlationRepository.save(
            ImageJobCorrelation(
                jobId = jobId,
                target = ImageProcessingTarget.PROFILE,
                ownerId = user.id.value,
                imageId = imageId,
            ),
        )
        imageProcessing.enqueue(
            ImageProcessingJob(
                jobId = jobId,
                target = ImageProcessingTarget.PROFILE,
                ownerId = user.id.value,
                imageId = imageId,
                sourceKey = originalKey,
                contentType = contentType,
            ),
        )

        // Nettoyage des objets de l'ancienne photo (original + variantes), tous dans le bucket privé.
        listOfNotNull(user.photoKey, user.photoThumbnailKey, user.photoFullKey)
            .forEach { imageStorage.delete(ImageVisibility.PRIVATE, it) }

        return UpdateProfilePhotoResult.Accepted(imageId)
    }
}
