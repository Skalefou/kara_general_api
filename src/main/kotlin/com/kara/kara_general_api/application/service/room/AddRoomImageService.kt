package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.application.service.image.ImageUploadPolicy
import com.kara.kara_general_api.domain.model.room.RoomImage
import com.kara.kara_general_api.domain.model.room.RoomImageId
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageCommand
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageResult
import com.kara.kara_general_api.domain.port.input.room.AddRoomImageUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.ImageVisibility
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AddRoomImageService(
    private val roomRepository: RoomRepository,
    private val imageStorage: ImageStoragePort,
) : AddRoomImageUseCase {

    @Transactional
    override fun addImage(command: AddRoomImageCommand): AddRoomImageResult {
        val contentType = command.contentType
        if (!ImageUploadPolicy.isAllowedType(contentType)) return AddRoomImageResult.InvalidImageType
        if (!ImageUploadPolicy.isWithinSize(command.bytes.size)) return AddRoomImageResult.ImageTooLarge

        val room = roomRepository.findById(command.roomId) ?: return AddRoomImageResult.RoomNotFound

        val extension = ImageUploadPolicy.extensionFor(contentType!!)
        val key = "rooms/${room.id.value}/${UUID.randomUUID()}.$extension"
        val position = room.images.size

        imageStorage.upload(ImageVisibility.PUBLIC, key, command.bytes, contentType)
        val image = roomRepository.addImage(room.id, RoomImage(RoomImageId.generate(), key, position))

        return AddRoomImageResult.Success(image, imageStorage.publicUrl(key))
    }
}
