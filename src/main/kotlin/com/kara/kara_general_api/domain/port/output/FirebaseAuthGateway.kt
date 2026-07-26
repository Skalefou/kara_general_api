package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.vo.Email

@JvmInline
value class FirebaseUserId(
    val value: String,
)

class EmailAlreadyUsedException(
    val email: Email,
) : RuntimeException("Email déjà utilisé : ${email.value}")

interface FirebaseAuthGateway {
    /**
     * @throws EmailAlreadyUsedException si l'email est déjà enregistré côté Firebase
     */
    fun createUser(
        email: Email,
        plainPassword: String,
    ): FirebaseUserId

    fun deleteUser(firebaseUserId: FirebaseUserId)

    fun updatePassword(
        firebaseUserId: FirebaseUserId,
        plainPassword: String,
    )

    /**
     * @throws EmailAlreadyUsedException si l'email est déjà enregistré côté Firebase
     */
    fun updateEmail(
        firebaseUserId: FirebaseUserId,
        email: Email,
    )
}
