package com.kara.kara_general_api.infrastructure.adapter.output.persistence.stock

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.stock.RoomStockEntry
import com.kara.kara_general_api.domain.model.stock.RoomStockItem
import com.kara.kara_general_api.domain.port.output.RoomStockRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RoomStockRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : RoomStockRepository {

    override fun findByRoomId(roomId: RoomId): List<RoomStockEntry> {
        val sql =
            """
            SELECT p.id, p.name, p.description, p.price, p.currency, rp.quantity
            FROM room_products rp
            JOIN products p ON p.id = rp.product_id
            WHERE rp.room_id = :roomId
            ORDER BY p.name ASC
            """.trimIndent()
        return jdbc.query(sql, mapOf("roomId" to roomId.value)) { rs, _ ->
            RoomStockEntry(
                product =
                    Product(
                        id = ProductId(rs.getObject("id", UUID::class.java)),
                        name = rs.getString("name"),
                        description = rs.getString("description"),
                        price = rs.getBigDecimal("price"),
                        currency = Currency.valueOf(rs.getString("currency")),
                    ),
                quantity = rs.getInt("quantity"),
            )
        }
    }

    override fun upsert(item: RoomStockItem) {
        val sql =
            """
            INSERT INTO room_products (id, room_id, product_id, quantity, created_at)
            VALUES (:id, :roomId, :productId, :quantity, NOW())
            ON CONFLICT ON CONSTRAINT uq_room_products_room_product DO UPDATE SET
                quantity = EXCLUDED.quantity
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("roomId", item.roomId.value)
                .addValue("productId", item.productId.value)
                .addValue("quantity", item.quantity),
        )
    }

    override fun deleteByRoomIdAndProductId(roomId: RoomId, productId: ProductId): Boolean {
        val sql = "DELETE FROM room_products WHERE room_id = :roomId AND product_id = :productId"
        val rows =
            jdbc.update(
                sql,
                mapOf("roomId" to roomId.value, "productId" to productId.value),
            )
        return rows > 0
    }
}
