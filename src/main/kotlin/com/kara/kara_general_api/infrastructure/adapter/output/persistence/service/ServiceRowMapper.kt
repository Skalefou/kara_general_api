package com.kara.kara_general_api.infrastructure.adapter.output.persistence.service

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.service.Service
import com.kara.kara_general_api.domain.model.service.ServiceId
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class ServiceRowMapper : RowMapper<Service> {
    override fun mapRow(rs: ResultSet, rowNum: Int): Service =
        Service(
            id = ServiceId(rs.getObject("id", UUID::class.java)),
            label = rs.getString("label"),
            description = rs.getString("description"),
            price = rs.getBigDecimal("price"),
            currency = Currency.valueOf(rs.getString("currency")),
        )
}
