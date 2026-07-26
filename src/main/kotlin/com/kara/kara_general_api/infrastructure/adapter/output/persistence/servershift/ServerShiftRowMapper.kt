package com.kara.kara_general_api.infrastructure.adapter.output.persistence.servershift

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.servershift.ServerShift
import com.kara.kara_general_api.domain.model.servershift.ServerShiftId
import com.kara.kara_general_api.domain.model.user.UserId
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class ServerShiftRowMapper : RowMapper<ServerShift> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): ServerShift =
        ServerShift(
            id = ServerShiftId(rs.getObject("id", UUID::class.java)),
            serverId = UserId(rs.getObject("server_id", UUID::class.java)),
            roomId = RoomId(rs.getObject("room_id", UUID::class.java)),
            startAt = rs.getTimestamp("start_at").toInstant(),
            endAt = rs.getTimestamp("end_at").toInstant(),
            note = rs.getString("note"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
}
