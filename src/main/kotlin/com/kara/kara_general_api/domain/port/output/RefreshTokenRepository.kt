package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.UserId
import java.util.UUID

data class RefreshToken(
    val value: String,
    val expiresInSeconds: Long,
)

interface RefreshTokenRepository {
    fun issue(userId: UserId): RefreshToken

    fun redeem(token: String): UUID?

    fun revoke(token: String)
}
