package com.kara.kara_general_api.domain.port.input.user

import com.kara.kara_general_api.domain.model.user.UserId

sealed interface DeleteProfilePhotoResult {
    data object Success : DeleteProfilePhotoResult

    data object UserNotFound : DeleteProfilePhotoResult
}

interface DeleteProfilePhotoUseCase {
    fun deletePhoto(userId: UserId): DeleteProfilePhotoResult
}
