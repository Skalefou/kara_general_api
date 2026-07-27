package com.kara.kara_general_api.application.service.auth.twofactor

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.MfaChallengeRepository
import java.time.Duration

/**
 * Double manuel de [MfaChallengeRepository] pour les tests unitaires du second facteur.
 *
 * Pourquoi pas MockK ici : `findUserId` retourne une `value class` **nullable** (`UserId?`), et le proxy
 * généré par MockK 1.13.13 échoue à la (dé)boxer (`ClassCastException: UserId cannot be cast to UUID`).
 * Un double écrit à la main contourne la limitation sans dégrader le typage du port.
 */
internal class FakeMfaChallengeRepository(
    private val token: String = "mfa-token",
) : MfaChallengeRepository {
    /** Utilisateur derrière le challenge ; `null` = challenge inconnu ou expiré. */
    var challengeOwner: UserId? = null

    /** Valeur que renverra le prochain appel à [incrementAttempts]. */
    var nextAttemptCount: Int = 1

    val issued = mutableListOf<Pair<UserId, Duration>>()
    val incrementedTokens = mutableListOf<String>()
    val deletedTokens = mutableListOf<String>()

    override fun issue(
        userId: UserId,
        ttl: Duration,
    ): String {
        issued += userId to ttl
        return token
    }

    override fun findUserId(token: String): UserId? = challengeOwner?.takeIf { token == this.token }

    override fun incrementAttempts(token: String): Int {
        incrementedTokens += token
        return nextAttemptCount
    }

    override fun delete(token: String) {
        deletedTokens += token
    }
}
