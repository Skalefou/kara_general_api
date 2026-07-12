package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.port.input.room.ListRoomsQuery
import com.kara.kara_general_api.domain.port.input.room.ListRoomsUseCase
import com.kara.kara_general_api.domain.port.input.room.RoomPage
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ListRoomsService(
    private val roomRepository: RoomRepository,
    @param:Value("\${kara.rooms.viewport.max-results:200}")
    private val viewportMaxResults: Int,
) : ListRoomsUseCase {

    override fun listRooms(query: ListRoomsQuery): RoomPage {
        val bbox = query.bbox ?: return listAllPaged(query)

        val totalInBbox = roomRepository.countInBbox(bbox)
        val rooms = roomRepository.findInBbox(bbox, viewportMaxResults)
        return RoomPage(
            rooms = rooms,
            page = query.page,
            size = query.size,
            totalElements = totalInBbox,
            totalInBbox = totalInBbox,
            truncated = totalInBbox > viewportMaxResults,
        )
    }

    private fun listAllPaged(query: ListRoomsQuery): RoomPage {
        val rooms = roomRepository.findAll(page = query.page, size = query.size)
        val totalElements = roomRepository.count()
        return RoomPage(rooms = rooms, page = query.page, size = query.size, totalElements = totalElements)
    }
}
