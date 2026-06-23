package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.vo.Email

@JvmInline
value class FirebaseUserId(val value: String)

interface FirebaseAuthGateway {
    fun createUser(email: Email, plainPassword: String): FirebaseUserId

    fun deleteUser(firebaseUserId: FirebaseUserId)
}