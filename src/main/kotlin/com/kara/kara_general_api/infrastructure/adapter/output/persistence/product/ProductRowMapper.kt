package com.kara.kara_general_api.infrastructure.adapter.output.persistence.product

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class ProductRowMapper : RowMapper<Product> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): Product =
        Product(
            id = ProductId(rs.getObject("id", UUID::class.java)),
            name = rs.getString("name"),
            description = rs.getString("description"),
            price = rs.getBigDecimal("price"),
            currency = Currency.valueOf(rs.getString("currency")),
        )
}
