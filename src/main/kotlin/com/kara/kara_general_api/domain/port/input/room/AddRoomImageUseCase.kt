package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.RoomId
import java.util.UUID

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
    /**
     * L'original a été accepté et le traitement des variantes est lancé de façon asynchrone.
     * [imageId] permet au client de suivre le statut ; l'image est en PROCESSING jusqu'au retour du worker.
     */
    data class Accepted(val imageId: UUID) : AddRoomImageResult

    data object RoomNotFound : AddRoomImageResult

    data object InvalidImageType : AddRoomImageResult

    data object ImageTooLarge : AddRoomImageResult
}

interface AddRoomImageUseCase {
    fun addImage(command: AddRoomImageCommand): AddRoomImageResult
}
