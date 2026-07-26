package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.port.input.room.GetRoomResult
import com.kara.kara_general_api.domain.port.input.room.GetRoomUseCase
import com.kara.kara_general_api.domain.port.output.RoomOptionRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.stereotype.Service

@Service
class GetRoomService(
    private val roomRepository: RoomRepository,
    private val roomOptionRepository: RoomOptionRepository,
) : GetRoomUseCase {
    override fun getRoom(id: RoomId): GetRoomResult {
        val room = roomRepository.findById(id) ?: return GetRoomResult.NotFound
        return GetRoomResult.Success(room, roomOptionRepository.findByRoomId(id))
    }
}
