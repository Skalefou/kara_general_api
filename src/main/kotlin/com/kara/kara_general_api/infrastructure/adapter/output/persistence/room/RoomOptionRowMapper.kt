package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOption
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class RoomOptionRowMapper : RowMapper<RoomOption> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): RoomOption =
        RoomOption(
            id = RoomOptionId(rs.getObject("id", UUID::class.java)),
            roomId = RoomId(rs.getObject("room_id", UUID::class.java)),
            label = rs.getString("label"),
            description = rs.getString("description"),
            price = rs.getBigDecimal("price"),
            currency = Currency.valueOf(rs.getString("currency")),
        )
}
