package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomImage

data class AddRoomImageCommand(
    val roomId: RoomId,
    val bytes: ByteArray,
    val contentType: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AddRoomImageCommand) return false
        return roomId == other.roomId &&
            contentType == other.contentType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = roomId.hashCode()
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

sealed interface AddRoomImageResult {
    /** [url] : URL publique (CDN) de l'image ajoutée. */
    data class Success(val image: RoomImage, val url: String) : AddRoomImageResult

    data object RoomNotFound : AddRoomImageResult

    data object InvalidImageType : AddRoomImageResult

    data object ImageTooLarge : AddRoomImageResult
}

interface AddRoomImageUseCase {
    fun addImage(command: AddRoomImageCommand): AddRoomImageResult
}
