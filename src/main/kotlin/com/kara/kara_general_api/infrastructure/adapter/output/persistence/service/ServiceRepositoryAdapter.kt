package com.kara.kara_general_api.infrastructure.adapter.output.persistence.service

import com.kara.kara_general_api.domain.model.service.Service
import com.kara.kara_general_api.domain.model.service.ServiceId
import com.kara.kara_general_api.domain.port.output.ServiceRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class ServiceRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: ServiceRowMapper,
) : ServiceRepository {
    override fun save(service: Service): Service {
        val sql =
            """
            INSERT INTO services (id, label, description, price, currency, created_at)
            VALUES (:id, :label, :description, :price, :currency, NOW())
            ON CONFLICT (id) DO UPDATE SET
                label       = EXCLUDED.label,
                description = EXCLUDED.description,
                price       = EXCLUDED.price,
                currency    = EXCLUDED.currency
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", service.id.value)
                .addValue("label", service.label)
                .addValue("description", service.description)
                .addValue("price", service.price)
                .addValue("currency", service.currency.name),
        )
        return service
    }

    override fun findAll(): List<Service> {
        val sql =
            """
            SELECT id, label, description, price, currency
            FROM services
            ORDER BY label ASC
            """.trimIndent()
        return jdbc.query(sql, rowMapper)
    }

    override fun findById(id: ServiceId): Service? {
        val sql =
            """
            SELECT id, label, description, price, currency
            FROM services
            WHERE id = :id
            """.trimIndent()
        return jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull()
    }

    override fun deleteById(id: ServiceId): Boolean {
        val sql = "DELETE FROM services WHERE id = :id"
        val rows = jdbc.update(sql, mapOf("id" to id.value))
        return rows > 0
    }

    override fun existsById(id: ServiceId): Boolean {
        val sql = "SELECT COUNT(*) FROM services WHERE id = :id"
        val count = jdbc.queryForObject(sql, mapOf("id" to id.value), Int::class.java) ?: 0
        return count > 0
    }
}
