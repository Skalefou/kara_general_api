package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCode
import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCodeId
import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorSecret
import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorStatus
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Jeux de données partagés par les tests unitaires des services A2F. */
internal object TwoFactorTestFixtures {
    val userId: UserId = UserId(UUID.randomUUID())

    val user: User =
        User(
            id = userId,
            email = Email("client@kara.app"),
            hashedPassword = HashedPassword("hashed"),
            firstName = "Marie",
            lastName = "Dupont",
            phoneNumber = PhoneNumber("0612345678"),
            birthDate = LocalDate.of(1995, 5, 20),
            role = UserRole.CLIENT,
            firebaseUid = "firebase-uid",
            createdAt = Instant.now(),
            emailVerified = true,
        )

    fun pendingSecret(cipher: String = "cipher"): TwoFactorSecret =
        TwoFactorSecret(
            userId = userId,
            secretCipher = cipher,
            status = TwoFactorStatus.PENDING,
            createdAt = Instant.now(),
        )

    fun activeSecret(
        cipher: String = "cipher",
        lastUsedStep: Long? = null,
    ): TwoFactorSecret =
        TwoFactorSecret(
            userId = userId,
            secretCipher = cipher,
            status = TwoFactorStatus.ACTIVE,
            createdAt = Instant.now(),
            activatedAt = Instant.now(),
            lastUsedStep = lastUsedStep,
        )

    fun recoveryCode(codeHash: String): RecoveryCode =
        RecoveryCode(
            id = RecoveryCodeId.generate(),
            userId = userId,
            codeHash = codeHash,
            createdAt = Instant.now(),
        )
}
