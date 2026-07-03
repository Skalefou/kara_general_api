package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp

@Component
class RoomRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: RoomRowMapper,
) : RoomRepository {

    override fun save(room: Room): Room {
        val sql =
            """
            INSERT INTO rooms (id, name, street, city, postal_code, country, created_at)
            VALUES (:id, :name, :street, :city, :postalCode, :country, :createdAt)
            ON CONFLICT (id) DO UPDATE SET
                name        = EXCLUDED.name,
                street      = EXCLUDED.street,
                city        = EXCLUDED.city,
                postal_code = EXCLUDED.postal_code,
                country     = EXCLUDED.country
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", room.id.value)
                .addValue("name", room.name)
                .addValue("street", room.address.street)
                .addValue("city", room.address.city)
                .addValue("postalCode", room.address.postalCode)
                .addValue("country", room.address.country)
                .addValue("createdAt", Timestamp.from(room.createdAt)),
        )
        return room
    }

    override fun findById(id: RoomId): Room? {
        val sql =
            """
            SELECT id, name, street, city, postal_code, country, created_at
            FROM rooms
            WHERE id = :id
            """.trimIndent()
        return jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull()
    }

    override fun findAll(page: Int, size: Int): List<Room> {
        val sql =
            """
            SELECT id, name, street, city, postal_code, country, created_at
            FROM rooms
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """.trimIndent()
        return jdbc.query(
            sql,
            mapOf("limit" to size, "offset" to page * size),
            rowMapper,
        )
    }

    override fun count(): Long {
        val sql = "SELECT COUNT(*) FROM rooms"
        return jdbc.queryForObject(sql, emptyMap<String, Any>(), Long::class.java) ?: 0
    }

    override fun deleteById(id: RoomId): Boolean {
        val sql = "DELETE FROM rooms WHERE id = :id"
        val rows = jdbc.update(sql, mapOf("id" to id.value))
        return rows > 0
    }
}
