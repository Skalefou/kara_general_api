package com.kara.kara_general_api.domain.model.user.twofactor

import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant
import java.util.UUID

@JvmInline
value class RecoveryCodeId(
    val value: UUID,
) {
    companion object {
        fun generate(): RecoveryCodeId = RecoveryCodeId(UUID.randomUUID())
    }
}

/**
 * Code de secours à usage unique, permettant de se connecter en cas de perte de l'application
 * d'authentification. Seul le haché ([codeHash], bcrypt) est persisté : le code en clair n'est affiché
 * qu'une seule fois, à sa génération.
 *
 * [usedAt] non nul = code déjà consommé, définitivement inutilisable.
 */
data class RecoveryCode(
    val id: RecoveryCodeId,
    val userId: UserId,
    val codeHash: String,
    val usedAt: Instant? = null,
    val createdAt: Instant,
) {
    val isUsed: Boolean get() = usedAt != null
}
