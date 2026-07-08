package com.kara.kara_general_api.domain.port.input.room

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomImageId

data class RemoveRoomImageCommand(
    val roomId: RoomId,
    val imageId: RoomImageId,
)

sealed interface RemoveRoomImageResult {
    data object Success : RemoveRoomImageResult

    data object RoomNotFound : RemoveRoomImageResult

    data object ImageNotFound : RemoveRoomImageResult
}

interface RemoveRoomImageUseCase {
    fun removeImage(command: RemoveRoomImageCommand): RemoveRoomImageResult
}
