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

    fun findAll(page: Int, size: Int): List<User>

    fun count(): Long

    fun markEmailVerified(id: UserId)

    /** Met à jour la clé de la photo de profil (null pour retirer la photo). */
    fun updatePhotoKey(id: UserId, photoKey: String?)

    fun anonymize(id: UserId)

    /** Applique un mot de passe définitif et lève le changement forcé (remet à zéro les champs temporaires). */
    fun updatePassword(id: UserId, hashedPassword: HashedPassword)

    /** Renouvelle l'invitation : nouveau mot de passe temporaire, changement forcé, nouvelle expiration. */
    fun applyReinvitation(id: UserId, hashedPassword: HashedPassword, tempPasswordExpiresAt: Instant)
}
