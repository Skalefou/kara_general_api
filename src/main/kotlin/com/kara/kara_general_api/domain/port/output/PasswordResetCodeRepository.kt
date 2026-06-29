package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.vo.Email
import java.time.Duration

interface PasswordResetCodeRepository {
    fun save(email: Email, code: String, ttl: Duration)

    fun find(email: Email): String?

    fun delete(email: Email)
}
