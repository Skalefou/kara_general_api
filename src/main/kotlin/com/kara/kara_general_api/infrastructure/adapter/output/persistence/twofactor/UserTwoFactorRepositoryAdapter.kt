package com.kara.kara_general_api.infrastructure.adapter.output.persistence.twofactor

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorSecret
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp

@Component
class UserTwoFactorRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: UserTwoFactorRowMapper,
) : TwoFactorRepository {
    override fun findByUserId(userId: UserId): TwoFactorSecret? {
        val sql =
            """
            SELECT user_id, secret_cipher, status, created_at, activated_at, last_used_step
            FROM user_two_factor
            WHERE user_id = :userId
            """.trimIndent()
        return jdbc.query(sql, mapOf("userId" to userId.value), rowMapper).firstOrNull()
    }

    /**
     * Insertion ou remplacement complet : la clé primaire étant `user_id`, un `PENDING` précédent est écrasé
     * par le nouveau secret (nouveau QR code ⇒ l'ancien devient caduc).
     */
    override fun save(secret: TwoFactorSecret): TwoFactorSecret {
        val sql =
            """
            INSERT INTO user_two_factor (user_id, secret_cipher, status, created_at, activated_at, last_used_step)
            VALUES (:userId, :secretCipher, :status, :createdAt, :activatedAt, :lastUsedStep)
            ON CONFLICT (user_id) DO UPDATE SET
                secret_cipher  = EXCLUDED.secret_cipher,
                status         = EXCLUDED.status,
                created_at     = EXCLUDED.created_at,
                activated_at   = EXCLUDED.activated_at,
                last_used_step = EXCLUDED.last_used_step
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("userId", secret.userId.value)
                .addValue("secretCipher", secret.secretCipher)
                .addValue("status", secret.status.name)
                .addValue("createdAt", Timestamp.from(secret.createdAt))
                .addValue("activatedAt", secret.activatedAt?.let(Timestamp::from))
                .addValue("lastUsedStep", secret.lastUsedStep),
        )
        return secret
    }

    override fun deleteByUserId(userId: UserId) {
        jdbc.update(
            "DELETE FROM user_two_factor WHERE user_id = :userId",
            mapOf("userId" to userId.value),
        )
    }

    override fun updateLastUsedStep(
        userId: UserId,
        step: Long,
    ) {
        val sql =
            """
            UPDATE user_two_factor
            SET last_used_step = :step
            WHERE user_id = :userId
            """.trimIndent()
        jdbc.update(sql, mapOf("userId" to userId.value, "step" to step))
    }
}
