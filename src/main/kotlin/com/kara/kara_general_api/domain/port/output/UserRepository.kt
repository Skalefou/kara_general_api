package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import java.time.Instant

interface UserRepository {
    fun existsByEmail(email: Email): Boolean

    fun save(user: User): User

    fun update(user: User): User

    fun findByEmail(email: Email): User?

    fun findByPhoneNumber(phoneNumber: PhoneNumber): User?

    fun findById(id: UserId): User?

    fun findAll(
        page: Int,
        size: Int,
    ): List<User>

    fun count(): Long

    fun markEmailVerified(id: UserId)

    /**
     * Enregistre l'original privé [originalKey] et bascule la photo en PROCESSING, en effaçant les clés de
     * variantes précédentes (une nouvelle photo invalide l'ancienne le temps du retraitement).
     */
    fun markPhotoProcessing(
        id: UserId,
        originalKey: String,
    )

    /** Marque la photo READY et enregistre les clés de variantes (idempotent : rejeu = écrasement). */
    fun markPhotoReady(
        id: UserId,
        thumbnailKey: String,
        fullKey: String,
    )

    /** Marque la photo FAILED (traitement worker en échec). */
    fun markPhotoFailed(id: UserId)

    /** Efface toute trace de photo de profil (original + variantes + statut). */
    fun clearPhoto(id: UserId)

    fun anonymize(id: UserId)

    /** Applique un mot de passe définitif et lève le changement forcé (remet à zéro les champs temporaires). */
    fun updatePassword(
        id: UserId,
        hashedPassword: HashedPassword,
    )

    /** Renouvelle l'invitation : nouveau mot de passe temporaire, changement forcé, nouvelle expiration. */
    fun applyReinvitation(
        id: UserId,
        hashedPassword: HashedPassword,
        tempPasswordExpiresAt: Instant,
    )

    /** Enregistre l'identifiant client Stripe (créé paresseusement au premier paiement). Jamais logué. */
    fun updateStripeCustomerId(
        id: UserId,
        stripeCustomerId: String,
    )

    /** Enregistre (ou remplace) le token d'appareil FCM utilisé pour les notifications push. */
    fun updateFcmToken(
        id: UserId,
        token: String,
    )
}
