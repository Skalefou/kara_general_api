package com.kara.kara_general_api.application.service.user

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.user.GetProfilePhotoResult
import com.kara.kara_general_api.domain.port.input.user.GetProfilePhotoUseCase
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import java.time.Duration

private val SIGNED_URL_TTL: Duration = Duration.ofMinutes(15)

@Service
class GetProfilePhotoService(
    private val userRepository: UserRepository,
    private val imageStorage: ImageStoragePort,
) : GetProfilePhotoUseCase {

    override fun getPhotoUrl(userId: UserId): GetProfilePhotoResult {
        val user = userRepository.findById(userId) ?: return GetProfilePhotoResult.UserNotFound
        val key = user.photoKey ?: return GetProfilePhotoResult.NoPhoto
        return GetProfilePhotoResult.Success(imageStorage.signedUrl(key, SIGNED_URL_TTL))
    }
}
