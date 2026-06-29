package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword

interface UserRepository {
    fun existsByEmail(email: Email): Boolean

    fun save(user: User): User

    fun findByEmail(email: Email): User?

    fun findById(id: UserId): User?

    fun markEmailVerified(id: UserId)

    fun anonymize(id: UserId)

    fun updatePassword(id: UserId, hashedPassword: HashedPassword)
}