package com.kara.kara_general_api.infrastructure.adapter.output.security

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.FirebaseUserId
import org.springframework.stereotype.Component

@Component
class FirebaseAuthAdapter(
    private val firebaseAuth: FirebaseAuth,
) : FirebaseAuthGateway {

    override fun createUser(email: Email, plainPassword: String): FirebaseUserId {
        val request =
            UserRecord.CreateRequest()
                .setEmail(email.value)
                .setPassword(plainPassword)
                .setEmailVerified(false)
        val userRecord = firebaseAuth.createUser(request)
        return FirebaseUserId(userRecord.uid)
    }

    override fun deleteUser(firebaseUserId: FirebaseUserId) {
        firebaseAuth.deleteUser(firebaseUserId.value)
    }
}