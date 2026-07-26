package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.application.service.image.ImageUploadPolicy
import com.kara.kara_general_api.domain.model.image.ImageJobCorrelation
import com.kara.kara_general_api.domain.model.image.ImageProcessingJob
import com.kara.kara_general_api.domain.model.image.ImageProcessingTarget
import com.kara.kara_general_api.domain.model.room.RoomImage
import com.kara.kara_general_api.domain.model.room.RoomImageId
import com.kara.kara_general_api.domain.model.room.RoomImageStatus
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageCommand
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageResult
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageUseCase
import com.kara.kara_general_api.domain.port.output.ImageJobCorrelationRepository
import com.kara.kara_general_api.domain.port.output.ImageProcessingPort
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.ImageVisibility
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Flux d'ajout d'image de salle **asynchrone** : on téléverse l'original dans le bucket privé, on persiste la
 * ligne en PROCESSING, on enregistre la corrélation et on publie un job. Les variantes publiques affichables
 * arrivent plus tard via `image-results`. La validation ([ImageUploadPolicy], 5 MiB + MIME) reste en amont
 * de tout enqueue.
 */
@Service
class AddRoomImageService(
    private val roomRepository: RoomRepository,
    private val imageStorage: ImageStoragePort,
    private val imageProcessing: ImageProcessingPort,
    private val correlationRepository: ImageJobCorrelationRepository,
) : AddRoomImageUseCase {
    @Transactional
    override fun addImage(command: AddRoomImageCommand): AddRoomImageResult {
        val contentType = command.contentType
        if (!ImageUploadPolicy.isAllowedType(contentType)) return AddRoomImageResult.InvalidImageType
        if (!ImageUploadPolicy.isWithinSize(command.bytes.size)) return AddRoomImageResult.ImageTooLarge

        val room = roomRepository.findById(command.roomId) ?: return AddRoomImageResult.RoomNotFound

        val imageId = UUID.randomUUID()
        val jobId = UUID.randomUUID()
        val extension = ImageUploadPolicy.extensionFor(contentType!!)
        val originalKey = "rooms/${room.id.value}/originals/$imageId.$extension"
        val position = room.images.size

        imageStorage.upload(ImageVisibility.PRIVATE, originalKey, command.bytes, contentType)
        roomRepository.addImage(
            room.id,
            RoomImage(
                id = RoomImageId(imageId),
                objectKey = originalKey,
                position = position,
                status = RoomImageStatus.PROCESSING,
            ),
        )
        correlationRepository.save(
            ImageJobCorrelation(
                jobId = jobId,
                target = ImageProcessingTarget.ROOM,
                ownerId = room.id.value,
                imageId = imageId,
            ),
        )
        imageProcessing.enqueue(
            ImageProcessingJob(
                jobId = jobId,
                target = ImageProcessingTarget.ROOM,
                ownerId = room.id.value,
                imageId = imageId,
                sourceKey = originalKey,
                contentType = contentType,
            ),
        )

        return AddRoomImageResult.Accepted(imageId)
    }
}
