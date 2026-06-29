package com.kara.kara_general_api.domain.model.user

import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import java.time.Instant
import java.time.LocalDate

data class User(
    val id: UserId,
    val email: Email,
    val hashedPassword: HashedPassword,
    val firstName: String,
    val lastName: String,
    val phoneNumber: PhoneNumber,
    val birthDate: LocalDate,
    val role: UserRole,
    val firebaseUid: String,
    val createdAt: Instant,
    val emailVerified: Boolean = false,
) {
    fun verifyEmail(): User = copy(emailVerified = true)

    fun changePassword(hashedPassword: HashedPassword): User = copy(hashedPassword = hashedPassword)

    companion object {
        fun register(
            email: Email,
            hashedPassword: HashedPassword,
            firstName: String,
            lastName: String,
            phoneNumber: PhoneNumber,
            birthDate: LocalDate,
            firebaseUid: String,
        ): User =
            User(
                id = UserId.generate(),
                email = email,
                hashedPassword = hashedPassword,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                birthDate = birthDate,
                role = UserRole.CLIENT,
                firebaseUid = firebaseUid,
                createdAt = Instant.now(),
                emailVerified = false,
            )
    }
}