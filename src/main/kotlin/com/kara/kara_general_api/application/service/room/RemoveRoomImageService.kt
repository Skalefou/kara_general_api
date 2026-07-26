package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.port.input.room.RemoveRoomImageCommand
import com.kara.kara_general_api.domain.port.input.room.RemoveRoomImageResult
import com.kara.kara_general_api.domain.port.input.room.RemoveRoomImageUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.ImageVisibility
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RemoveRoomImageService(
    private val roomRepository: RoomRepository,
    private val imageStorage: ImageStoragePort,
) : RemoveRoomImageUseCase {
    @Transactional
    override fun removeImage(command: RemoveRoomImageCommand): RemoveRoomImageResult {
        val room = roomRepository.findById(command.roomId) ?: return RemoveRoomImageResult.RoomNotFound
        val image = room.images.find { it.id == command.imageId } ?: return RemoveRoomImageResult.ImageNotFound

        roomRepository.removeImage(room.id, image.id)
        // L'original est dans le bucket privé ; les variantes affichables dans le bucket public.
        imageStorage.delete(ImageVisibility.PRIVATE, image.objectKey)
        image.variants.forEach { imageStorage.delete(ImageVisibility.PUBLIC, it.objectKey) }

        return RemoveRoomImageResult.Success
    }
}
