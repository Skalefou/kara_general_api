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
        val sql =
            """
            SELECT id, room_id, label, description, price, currency
            FROM room_options
            WHERE room_id = :roomId
            ORDER BY label ASC
            """.trimIndent()
        return jdbc.query(sql, mapOf("roomId" to roomId.value), rowMapper)
    }
}
