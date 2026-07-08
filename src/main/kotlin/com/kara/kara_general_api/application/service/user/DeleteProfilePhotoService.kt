package com.kara.kara_general_api.application.service.user

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.user.DeleteProfilePhotoResult
import com.kara.kara_general_api.domain.port.input.user.DeleteProfilePhotoUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.ImageVisibility
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteProfilePhotoService(
    private val userRepository: UserRepository,
    private val imageStorage: ImageStoragePort,
) : DeleteProfilePhotoUseCase {

    @Transactional
    override fun deletePhoto(userId: UserId): DeleteProfilePhotoResult {
        val user = userRepository.findById(userId) ?: return DeleteProfilePhotoResult.UserNotFound
        user.photoKey?.let { key ->
            userRepository.updatePhotoKey(user.id, null)
            imageStorage.delete(ImageVisibility.PRIVATE, key)
        }
        return DeleteProfilePhotoResult.Success
    }
}
