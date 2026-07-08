package com.kara.kara_general_api.domain.port.input.user

import com.kara.kara_general_api.domain.model.user.UserId

sealed interface GetProfilePhotoResult {
    /** [photoUrl] : URL signée courte durée vers la photo de profil. */
    data class Success(val photoUrl: String) : GetProfilePhotoResult

    data object UserNotFound : GetProfilePhotoResult

    data object NoPhoto : GetProfilePhotoResult
}

interface GetProfilePhotoUseCase {
    fun getPhotoUrl(userId: UserId): GetProfilePhotoResult
}
