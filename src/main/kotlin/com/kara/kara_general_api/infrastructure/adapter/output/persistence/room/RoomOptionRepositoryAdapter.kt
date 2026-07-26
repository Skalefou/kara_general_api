package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOption
import com.kara.kara_general_api.domain.port.output.RoomOptionRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class RoomOptionRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: RoomOptionRowMapper,
) : RoomOptionRepository {
    override fun findByRoomId(roomId: RoomId): List<RoomOption> {
        // Catalogue global : les services attachés à la salle via la liaison room_services.
        // On projette chaque service en RoomOption (id = id du service) afin de conserver la forme
        // du contrat RoomResponse.options[] inchangée pour les fronts.
        val sql =
            """
            SELECT s.id AS id, rs.room_id AS room_id, s.label AS label,
                   s.description AS description, s.price AS price, s.currency AS currency
            FROM room_services rs
            JOIN services s ON s.id = rs.service_id
            WHERE rs.room_id = :roomId
            ORDER BY s.label ASC
            """.trimIndent()
        return jdbc.query(sql, mapOf("roomId" to roomId.value), rowMapper)
    }
}
