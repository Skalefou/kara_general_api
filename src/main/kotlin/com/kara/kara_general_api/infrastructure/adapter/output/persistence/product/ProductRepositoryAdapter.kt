package com.kara.kara_general_api.infrastructure.adapter.output.persistence.product

import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.port.output.ProductRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class ProductRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: ProductRowMapper,
) : ProductRepository {
    override fun save(product: Product): Product {
        val sql =
            """
            INSERT INTO products (id, name, description, price, currency, created_at)
            VALUES (:id, :name, :description, :price, :currency, NOW())
            ON CONFLICT (id) DO UPDATE SET
                name        = EXCLUDED.name,
                description = EXCLUDED.description,
                price       = EXCLUDED.price,
                currency    = EXCLUDED.currency
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", product.id.value)
                .addValue("name", product.name)
                .addValue("description", product.description)
                .addValue("price", product.price)
                .addValue("currency", product.currency.name),
        )
        return product
    }

    override fun findAll(): List<Product> {
        val sql =
            """
            SELECT id, name, description, price, currency
            FROM products
            ORDER BY name ASC
            """.trimIndent()
        return jdbc.query(sql, rowMapper)
    }

    override fun findById(id: ProductId): Product? {
        val sql =
            """
            SELECT id, name, description, price, currency
            FROM products
            WHERE id = :id
            """.trimIndent()
        return jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull()
    }

    override fun deleteById(id: ProductId): Boolean {
        val sql = "DELETE FROM products WHERE id = :id"
        val rows = jdbc.update(sql, mapOf("id" to id.value))
        return rows > 0
    }

    override fun existsById(id: ProductId): Boolean {
        val sql = "SELECT COUNT(*) FROM products WHERE id = :id"
        val count = jdbc.queryForObject(sql, mapOf("id" to id.value), Int::class.java) ?: 0
        return count > 0
    }
}
