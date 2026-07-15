package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomStatus
import com.kara.kara_general_api.domain.model.room.vo.Address
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class RoomRowMapper : RowMapper<Room> {
    override fun mapRow(rs: ResultSet, rowNum: Int): Room =
        Room(
            id = RoomId(rs.getObject("id", UUID::class.java)),
            name = rs.getString("name"),
            description = rs.getString("description"),
            address =
                Address(
                    street = rs.getString("street"),
                    city = rs.getString("city"),
                    postalCode = rs.getString("postal_code"),
                    country = rs.getString("country"),
                ),
            pricePerPersonPerHour = rs.getBigDecimal("price_per_person_per_hour"),
            currency = Currency.valueOf(rs.getString("currency")),
            isThereWifi = rs.getBoolean("is_there_wifi"),
            isThereSonoPro = rs.getBoolean("is_there_sono_pro"),
            isThereAirConditioning = rs.getBoolean("is_there_air_conditioning"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            status = RoomStatus.valueOf(rs.getString("status")),
            latitude = rs.getObject("latitude", Double::class.javaObjectType),
            longitude = rs.getObject("longitude", Double::class.javaObjectType),
        )
}
