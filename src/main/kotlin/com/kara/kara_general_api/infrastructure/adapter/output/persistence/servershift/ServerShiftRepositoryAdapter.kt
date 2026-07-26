package com.kara.kara_general_api.infrastructure.adapter.output.persistence.servershift

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.servershift.ServerShift
import com.kara.kara_general_api.domain.model.servershift.ServerShiftId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant

private const val SHIFT_COLUMNS = "id, server_id, room_id, start_at, end_at, note, created_at"

@Component
class ServerShiftRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: ServerShiftRowMapper,
) : ServerShiftRepository {
    override fun save(shift: ServerShift): ServerShift {
        val sql =
            """
            INSERT INTO server_shifts (id, server_id, room_id, start_at, end_at, note, created_at)
            VALUES (:id, :serverId, :roomId, :startAt, :endAt, :note, :createdAt)
            ON CONFLICT (id) DO UPDATE SET
                room_id  = EXCLUDED.room_id,
                start_at = EXCLUDED.start_at,
                end_at   = EXCLUDED.end_at,
                note     = EXCLUDED.note
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", shift.id.value)
                .addValue("serverId", shift.serverId.value)
                .addValue("roomId", shift.roomId.value)
                .addValue("startAt", Timestamp.from(shift.startAt))
                .addValue("endAt", Timestamp.from(shift.endAt))
                .addValue("note", shift.note)
                .addValue("createdAt", Timestamp.from(shift.createdAt)),
        )
        return shift
    }

    override fun findById(id: ServerShiftId): ServerShift? {
        val sql =
            """
            SELECT $SHIFT_COLUMNS
            FROM server_shifts
            WHERE id = :id
            """.trimIndent()
        return jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull()
    }

    override fun findAll(
        serverId: UserId?,
        roomId: RoomId?,
        from: Instant?,
        to: Instant?,
    ): List<ServerShift> {
        // Filtres optionnels : chaque critère null est neutralisé par `(:param IS NULL OR ...)`.
        // Fenêtre temporelle : chevauchement du créneau [start_at, end_at) avec [from, to).
        val sql =
            """
            SELECT $SHIFT_COLUMNS
            FROM server_shifts
            WHERE (CAST(:serverId AS uuid) IS NULL OR server_id = CAST(:serverId AS uuid))
              AND (CAST(:roomId AS uuid) IS NULL OR room_id = CAST(:roomId AS uuid))
              AND (CAST(:from AS timestamptz) IS NULL OR end_at > CAST(:from AS timestamptz))
              AND (CAST(:to AS timestamptz) IS NULL OR start_at < CAST(:to AS timestamptz))
            ORDER BY start_at ASC
            """.trimIndent()
        return jdbc.query(
            sql,
            MapSqlParameterSource()
                .addValue("serverId", serverId?.value)
                .addValue("roomId", roomId?.value)
                .addValue("from", from?.let { Timestamp.from(it) })
                .addValue("to", to?.let { Timestamp.from(it) }),
            rowMapper,
        )
    }

    override fun existsOverlappingForServer(
        serverId: UserId,
        startAt: Instant,
        endAt: Instant,
        excludeId: ServerShiftId?,
    ): Boolean {
        // Chevauchement de créneaux : deux intervalles [a,b) et [c,d) se chevauchent ssi a < d ET b > c.
        val sql =
            """
            SELECT COUNT(*)
            FROM server_shifts
            WHERE server_id = :serverId
              AND (CAST(:excludeId AS uuid) IS NULL OR id <> CAST(:excludeId AS uuid))
              AND start_at < :endAt
              AND end_at > :startAt
            """.trimIndent()
        val count =
            jdbc.queryForObject(
                sql,
                MapSqlParameterSource()
                    .addValue("serverId", serverId.value)
                    .addValue("excludeId", excludeId?.value)
                    .addValue("startAt", Timestamp.from(startAt))
                    .addValue("endAt", Timestamp.from(endAt)),
                Int::class.java,
            ) ?: 0
        return count > 0
    }

    override fun deleteById(id: ServerShiftId): Boolean {
        val sql = "DELETE FROM server_shifts WHERE id = :id"
        return jdbc.update(sql, mapOf("id" to id.value)) > 0
    }

    override fun findServerIdsAssignedTo(
        roomId: RoomId,
        startAt: Instant,
        endAt: Instant,
    ): Set<UserId> {
        // Serveurs dont un créneau de la même salle chevauche [startAt, endAt) (a<d ET b>c).
        val sql =
            """
            SELECT DISTINCT server_id
            FROM server_shifts
            WHERE room_id = :roomId
              AND start_at < :endAt
              AND end_at > :startAt
            """.trimIndent()
        return jdbc
            .query(
                sql,
                MapSqlParameterSource()
                    .addValue("roomId", roomId.value)
                    .addValue("startAt", Timestamp.from(startAt))
                    .addValue("endAt", Timestamp.from(endAt)),
            ) { rs, _ -> UserId(rs.getObject("server_id", java.util.UUID::class.java)) }
            .toSet()
    }
}
