package com.kara.kara_general_api.infrastructure.adapter.output.persistence.favorite

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.RoomFavoriteRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RoomFavoriteRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : RoomFavoriteRepository {
    // DO NOTHING sur la contrainte d'unicité : un second ajout du même favori ne renvoie aucune ligne
    // modifiée, ce qui rend l'opération idempotente sans lecture préalable.
    override fun add(
        userId: UserId,
        roomId: RoomId,
    ): Boolean {
        val sql =
            """
            INSERT INTO room_favorites (id, user_id, room_id, created_at)
            VALUES (:id, :userId, :roomId, NOW())
            ON CONFLICT (user_id, room_id) DO NOTHING
            """.trimIndent()
        val rows =
            jdbc.update(
                sql,
                MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("userId", userId.value)
                    .addValue("roomId", roomId.value),
            )
        return rows > 0
    }

    override fun remove(
        userId: UserId,
        roomId: RoomId,
    ): Boolean {
        val sql = "DELETE FROM room_favorites WHERE user_id = :userId AND room_id = :roomId"
        val rows = jdbc.update(sql, mapOf("userId" to userId.value, "roomId" to roomId.value))
        return rows > 0
    }

    override fun findRoomIdsByUser(
        userId: UserId,
        page: Int,
        size: Int,
    ): List<RoomId> {
        val sql =
            """
            SELECT room_id
            FROM room_favorites
            WHERE user_id = :userId
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """.trimIndent()
        val params =
            mapOf(
                "userId" to userId.value,
                "limit" to size,
                "offset" to page.toLong() * size,
            )
        return jdbc.query(sql, params) { rs, _ -> RoomId(rs.getObject("room_id", UUID::class.java)) }
    }

    override fun findAllRoomIdsByUser(userId: UserId): List<RoomId> {
        val sql =
            """
            SELECT room_id
            FROM room_favorites
            WHERE user_id = :userId
            ORDER BY created_at DESC
            """.trimIndent()
        return jdbc.query(sql, mapOf("userId" to userId.value)) { rs, _ ->
            RoomId(rs.getObject("room_id", UUID::class.java))
        }
    }

    override fun countByUser(userId: UserId): Long {
        val sql = "SELECT COUNT(*) FROM room_favorites WHERE user_id = :userId"
        return jdbc.queryForObject(sql, mapOf("userId" to userId.value), Long::class.java) ?: 0
    }
}
