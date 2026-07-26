package com.kara.kara_general_api.infrastructure.adapter.output.persistence.twofactor

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorSecret
import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorStatus
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class UserTwoFactorRowMapper : RowMapper<TwoFactorSecret> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): TwoFactorSecret =
        TwoFactorSecret(
            userId = UserId(rs.getObject("user_id", UUID::class.java)),
            secretCipher = rs.getString("secret_cipher"),
            status = TwoFactorStatus.valueOf(rs.getString("status")),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            activatedAt = rs.getTimestamp("activated_at")?.toInstant(),
            // Colonne BIGINT nullable : getLong() renvoie 0 pour un NULL, d'où le contrôle wasNull().
            lastUsedStep = rs.getLong("last_used_step").takeUnless { rs.wasNull() },
        )
}
