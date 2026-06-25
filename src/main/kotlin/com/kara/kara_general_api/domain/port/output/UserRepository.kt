package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email

interface UserRepository {
    fun existsByEmail(email: Email): Boolean

    fun save(user: User): User

    fun findByEmail(email: Email): User?

    fun markEmailVerified(id: UserId)
}