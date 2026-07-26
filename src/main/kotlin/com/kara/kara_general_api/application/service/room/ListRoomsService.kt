package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.vo.BoundingBox
import com.kara.kara_general_api.domain.port.input.room.ListRoomsQuery
import com.kara.kara_general_api.domain.port.input.room.ListRoomsUseCase
import com.kara.kara_general_api.domain.port.input.room.RoomPage
import com.kara.kara_general_api.domain.port.input.room.ViewportMode
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ListRoomsService(
    private val roomRepository: RoomRepository,
    @param:Value("\${kara.rooms.viewport.max-results:1000}")
    private val viewportMaxResults: Int,
    @param:Value("\${kara.rooms.viewport.cluster-grid-size:8}")
    private val clusterGridSize: Int,
) : ListRoomsUseCase {
    override fun listRooms(query: ListRoomsQuery): RoomPage {
        val bbox = query.bbox ?: return listAllPaged(query)

        val totalInBbox = roomRepository.countInBbox(bbox)
        return if (totalInBbox <= viewportMaxResults) {
            roomsView(query, bbox, totalInBbox)
        } else {
            clustersView(query, bbox, totalInBbox)
        }
    }

    // totalInBbox <= cap : toutes les salles tiennent sous le plafond, aucune troncature.
    private fun roomsView(
        query: ListRoomsQuery,
        bbox: BoundingBox,
        totalInBbox: Long,
    ): RoomPage {
        val rooms = roomRepository.findInBbox(bbox, viewportMaxResults)
        return RoomPage(
            rooms = rooms,
            page = query.page,
            size = query.size,
            totalElements = totalInBbox,
            totalInBbox = totalInBbox,
            truncated = false,
            mode = ViewportMode.ROOMS,
            clusters = emptyList(),
        )
    }

    // totalInBbox > cap : on agrège en clusters ; tout est représenté donc truncated = false.
    private fun clustersView(
        query: ListRoomsQuery,
        bbox: BoundingBox,
        totalInBbox: Long,
    ): RoomPage {
        val clusters = roomRepository.clustersInBbox(bbox, clusterGridSize)
        return RoomPage(
            rooms = emptyList(),
            page = query.page,
            size = query.size,
            totalElements = totalInBbox,
            totalInBbox = totalInBbox,
            truncated = false,
            mode = ViewportMode.CLUSTERS,
            clusters = clusters,
        )
    }

    private fun listAllPaged(query: ListRoomsQuery): RoomPage {
        val rooms = roomRepository.findAll(page = query.page, size = query.size)
        val totalElements = roomRepository.count()
        return RoomPage(rooms = rooms, page = query.page, size = query.size, totalElements = totalElements)
    }
}
