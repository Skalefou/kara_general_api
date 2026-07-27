package com.kara.kara_general_api.infrastructure.adapter.output.persistence.twofactor

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCode
import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCodeId
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class UserRecoveryCodeRowMapper : RowMapper<RecoveryCode> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): RecoveryCode =
        RecoveryCode(
            id = RecoveryCodeId(rs.getObject("id", UUID::class.java)),
            userId = UserId(rs.getObject("user_id", UUID::class.java)),
            codeHash = rs.getString("code_hash"),
            usedAt = rs.getTimestamp("used_at")?.toInstant(),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
}
