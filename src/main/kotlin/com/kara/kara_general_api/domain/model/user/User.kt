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
    val deletedAt: Instant? = null,
    val deactivatedAt: Instant? = null,
    val mustChangePassword: Boolean = false,
    val tempPasswordExpiresAt: Instant? = null,
) {
    fun verifyEmail(): User = copy(emailVerified = true)

    fun updateProfile(
        firstName: String?,
        lastName: String?,
        phoneNumber: PhoneNumber?,
        birthDate: LocalDate?,
    ): User =
        copy(
            firstName = firstName ?: this.firstName,
            lastName = lastName ?: this.lastName,
            phoneNumber = phoneNumber ?: this.phoneNumber,
            birthDate = birthDate ?: this.birthDate,
        )

    fun changeEmail(newEmail: Email): User = copy(email = newEmail, emailVerified = false)

    fun deactivate(): User = copy(deactivatedAt = Instant.now())

    /** Renouvelle l'invitation : nouveau mot de passe temporaire, changement forcé, expiration remise à jour. */
    fun reinvited(newHashedPassword: HashedPassword, tempPasswordExpiresAt: Instant): User =
        copy(
            hashedPassword = newHashedPassword,
            mustChangePassword = true,
            tempPasswordExpiresAt = tempPasswordExpiresAt,
        )

    /** Applique un mot de passe définitif choisi par l'utilisateur et lève le changement forcé. */
    fun passwordChanged(newHashedPassword: HashedPassword): User =
        copy(
            hashedPassword = newHashedPassword,
            mustChangePassword = false,
            tempPasswordExpiresAt = null,
        )

    fun isTempPasswordExpired(now: Instant): Boolean =
        mustChangePassword && tempPasswordExpiresAt != null && tempPasswordExpiresAt.isBefore(now)

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

        /**
         * Compte serveur créé par un administrateur : rôle SERVER, email considéré de confiance
         * (pas de flux de vérification), mot de passe temporaire à changer à la première connexion.
         */
        fun createServerAccount(
            email: Email,
            hashedPassword: HashedPassword,
            firstName: String,
            lastName: String,
            phoneNumber: PhoneNumber,
            birthDate: LocalDate,
            firebaseUid: String,
            tempPasswordExpiresAt: Instant,
        ): User =
            User(
                id = UserId.generate(),
                email = email,
                hashedPassword = hashedPassword,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                birthDate = birthDate,
                role = UserRole.SERVER,
                firebaseUid = firebaseUid,
                createdAt = Instant.now(),
                emailVerified = true,
                mustChangePassword = true,
                tempPasswordExpiresAt = tempPasswordExpiresAt,
            )
    }
}
