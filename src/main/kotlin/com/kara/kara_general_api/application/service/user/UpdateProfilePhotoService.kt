package com.kara.kara_general_api.application.service.user

import com.kara.kara_general_api.application.service.image.ImageUploadPolicy
import com.kara.kara_general_api.domain.port.input.user.UpdateProfilePhotoCommand
import com.kara.kara_general_api.domain.port.input.user.UpdateProfilePhotoResult
import com.kara.kara_general_api.domain.port.input.user.UpdateProfilePhotoUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.ImageVisibility
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

private val SIGNED_URL_TTL: Duration = Duration.ofMinutes(15)

@Service
class UpdateProfilePhotoService(
    private val userRepository: UserRepository,
    private val imageStorage: ImageStoragePort,
) : UpdateProfilePhotoUseCase {

    @Transactional
    override fun updatePhoto(command: UpdateProfilePhotoCommand): UpdateProfilePhotoResult {
        val contentType = command.contentType
        if (!ImageUploadPolicy.isAllowedType(contentType)) return UpdateProfilePhotoResult.InvalidImageType
        if (!ImageUploadPolicy.isWithinSize(command.bytes.size)) return UpdateProfilePhotoResult.ImageTooLarge

        val user = userRepository.findById(command.userId) ?: return UpdateProfilePhotoResult.UserNotFound

        val extension = ImageUploadPolicy.extensionFor(contentType!!)
        val newKey = "profiles/${user.id.value}/${UUID.randomUUID()}.$extension"

        imageStorage.upload(ImageVisibility.PRIVATE, newKey, command.bytes, contentType)
        userRepository.updatePhotoKey(user.id, newKey)

        // Nettoyage de l'ancienne photo une fois la nouvelle référencée.
        user.photoKey?.let { imageStorage.delete(ImageVisibility.PRIVATE, it) }

        return UpdateProfilePhotoResult.Success(imageStorage.signedUrl(newKey, SIGNED_URL_TTL))
    }
}
