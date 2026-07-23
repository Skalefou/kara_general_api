package com.kara.kara_general_api.infrastructure.adapter.output.persistence.image

import com.kara.kara_general_api.domain.model.image.ImageJobCorrelation
import com.kara.kara_general_api.domain.model.image.ImageProcessingTarget
import com.kara.kara_general_api.domain.port.output.ImageJobCorrelationRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ImageJobCorrelationRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : ImageJobCorrelationRepository {

    override fun save(correlation: ImageJobCorrelation) {
        // ON CONFLICT : robustesse en cas de rejeu applicatif du même job (clé déterministe).
        val sql =
            """
            INSERT INTO image_jobs (job_id, target, owner_id, image_id, created_at)
            VALUES (:jobId, :target, :ownerId, :imageId, NOW())
            ON CONFLICT (job_id) DO NOTHING
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("jobId", correlation.jobId)
                .addValue("target", correlation.target.name)
                .addValue("ownerId", correlation.ownerId)
                .addValue("imageId", correlation.imageId),
        )
    }

    override fun findByJobId(jobId: UUID): ImageJobCorrelation? {
        val sql =
            """
            SELECT job_id, target, owner_id, image_id
            FROM image_jobs
            WHERE job_id = :jobId
            """.trimIndent()
        return jdbc.query(sql, mapOf("jobId" to jobId)) { rs, _ ->
            ImageJobCorrelation(
                jobId = rs.getObject("job_id", UUID::class.java),
                target = ImageProcessingTarget.valueOf(rs.getString("target")),
                ownerId = rs.getObject("owner_id", UUID::class.java),
                imageId = rs.getObject("image_id", UUID::class.java),
            )
        }.firstOrNull()
    }
}
