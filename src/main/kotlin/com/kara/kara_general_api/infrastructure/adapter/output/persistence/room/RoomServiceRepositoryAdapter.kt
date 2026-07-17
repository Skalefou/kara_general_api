package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.service.ServiceId
import com.kara.kara_general_api.domain.port.output.RoomServiceRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RoomServiceRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : RoomServiceRepository {

    override fun addLinks(roomId: RoomId, serviceIds: List<ServiceId>) {
        if (serviceIds.isEmpty()) return
        // ON CONFLICT sur la contrainte UNIQUE(room_id, service_id) : réattacher un service déjà lié
        // est idempotent et ne lève pas d'erreur.
        val sql =
            """
            INSERT INTO room_services (id, room_id, service_id, created_at)
            VALUES (:id, :roomId, :serviceId, NOW())
            ON CONFLICT ON CONSTRAINT uq_room_services_room_service DO NOTHING
            """.trimIndent()
        val batch =
            serviceIds.map { serviceId ->
                MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("roomId", roomId.value)
                    .addValue("serviceId", serviceId.value)
            }.toTypedArray()
        jdbc.batchUpdate(sql, batch)
    }

    override fun replaceLinks(roomId: RoomId, serviceIds: List<ServiceId>) {
        deleteByRoomId(roomId)
        addLinks(roomId, serviceIds)
    }

    override fun deleteByRoomId(roomId: RoomId): Int {
        val sql = "DELETE FROM room_services WHERE room_id = :roomId"
        return jdbc.update(sql, mapOf("roomId" to roomId.value))
    }

    override fun findServiceIdsByRoomId(roomId: RoomId): List<ServiceId> {
        val sql =
            """
            SELECT service_id
            FROM room_services
            WHERE room_id = :roomId
            ORDER BY created_at ASC
            """.trimIndent()
        return jdbc.query(sql, mapOf("roomId" to roomId.value)) { rs, _ ->
            ServiceId(rs.getObject("service_id", UUID::class.java))
        }
    }
}
