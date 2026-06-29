package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.vo.HashedPassword

interface PasswordHasher {
    fun hash(plainPassword: String): HashedPassword

    fun matches(plainPassword: String, hashedPassword: HashedPassword): Boolean
}