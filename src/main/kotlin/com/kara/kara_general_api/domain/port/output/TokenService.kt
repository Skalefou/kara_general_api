package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.User

data class AccessToken(
    val value: String,
    val expiresInSeconds: Long,
)

interface TokenService {
    fun generateAccessToken(user: User): AccessToken
}
