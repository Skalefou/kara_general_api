package com.kara.kara_general_api.domain.port.input.user

import com.kara.kara_general_api.domain.model.user.PhotoStatus
import com.kara.kara_general_api.domain.model.user.UserId

sealed interface GetProfilePhotoResult {
    /**
     * [status] reflète l'avancement du traitement. Les URL signées par variante ([thumbnailUrl], [fullUrl])
     * ne sont renseignées que lorsque [status] == READY ; sinon elles sont nulles (le client affiche un état
     * de chargement ou d'échec).
     */
    data class Success(
        val status: PhotoStatus,
        val thumbnailUrl: String?,
        val fullUrl: String?,
    ) : GetProfilePhotoResult

    data object UserNotFound : GetProfilePhotoResult

    data object NoPhoto : GetProfilePhotoResult
}

interface GetProfilePhotoUseCase {
    fun getPhotoUrl(userId: UserId): GetProfilePhotoResult
}
