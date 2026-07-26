package com.kara.kara_general_api.domain.port.input.user

import com.kara.kara_general_api.domain.model.user.UserId
import java.util.UUID

data class UpdateProfilePhotoCommand(
    val userId: UserId,
    val bytes: ByteArray,
    val contentType: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UpdateProfilePhotoCommand) return false
        return userId == other.userId &&
            contentType == other.contentType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

sealed interface UpdateProfilePhotoResult {
    /**
     * L'original a été accepté ; les variantes (thumbnail/full) sont générées de façon asynchrone.
     * La photo reste en PROCESSING jusqu'au retour du worker. [imageId] permet d'en suivre le statut.
     */
    data class Accepted(
        val imageId: UUID,
    ) : UpdateProfilePhotoResult

    data object UserNotFound : UpdateProfilePhotoResult

    data object InvalidImageType : UpdateProfilePhotoResult

    data object ImageTooLarge : UpdateProfilePhotoResult
}

interface UpdateProfilePhotoUseCase {
    fun updatePhoto(command: UpdateProfilePhotoCommand): UpdateProfilePhotoResult
}
