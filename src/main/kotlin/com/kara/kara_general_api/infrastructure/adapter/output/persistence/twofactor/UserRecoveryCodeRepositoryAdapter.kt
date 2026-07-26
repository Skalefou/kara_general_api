package com.kara.kara_general_api.infrastructure.adapter.output.persistence.twofactor

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCode
import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCodeId
import com.kara.kara_general_api.domain.port.output.RecoveryCodeRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserRecoveryCodeRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: UserRecoveryCodeRowMapper,
) : RecoveryCodeRepository {
    /**
     * Remplace l'intégralité de la série : les anciens codes (consommés ou non) sont supprimés avant
     * insertion des nouveaux. Appelé sous `@Transactional` côté service, donc atomique.
     */
    override fun replaceAll(
        userId: UserId,
        codeHashes: List<String>,
    ) {
        deleteByUserId(userId)
        if (codeHashes.isEmpty()) {
            return
        }
        val sql =
            """
            INSERT INTO user_recovery_codes (id, user_id, code_hash, used_at, created_at)
            VALUES (:id, :userId, :codeHash, NULL, NOW())
            """.trimIndent()
        val batch =
            codeHashes
                .map { codeHash ->
                    MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("userId", userId.value)
                        .addValue("codeHash", codeHash)
                }.toTypedArray()
        jdbc.batchUpdate(sql, batch)
    }

    override fun findUnusedByUserId(userId: UserId): List<RecoveryCode> {
        val sql =
            """
            SELECT id, user_id, code_hash, used_at, created_at
            FROM user_recovery_codes
            WHERE user_id = :userId
              AND used_at IS NULL
            ORDER BY created_at ASC
            """.trimIndent()
        return jdbc.query(sql, mapOf("userId" to userId.value), rowMapper)
    }

    /** Consommation à usage unique : le `used_at IS NULL` empêche de « re-consommer » un code déjà servi. */
    override fun markUsed(id: RecoveryCodeId) {
        val sql =
            """
            UPDATE user_recovery_codes
            SET used_at = NOW()
            WHERE id = :id
              AND used_at IS NULL
            """.trimIndent()
        jdbc.update(sql, mapOf("id" to id.value))
    }

    override fun deleteByUserId(userId: UserId) {
        jdbc.update(
            "DELETE FROM user_recovery_codes WHERE user_id = :userId",
            mapOf("userId" to userId.value),
        )
    }

    override fun countUnused(userId: UserId): Int {
        val sql =
            """
            SELECT count(*)
            FROM user_recovery_codes
            WHERE user_id = :userId
              AND used_at IS NULL
            """.trimIndent()
        return jdbc.queryForObject(sql, mapOf("userId" to userId.value), Int::class.java) ?: 0
    }
}
