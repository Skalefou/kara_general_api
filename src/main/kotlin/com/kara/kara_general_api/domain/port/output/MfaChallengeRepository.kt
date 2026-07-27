package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Duration

/**
 * Challenge A2F : jeton de courte durée émis lorsque le mot de passe est validé mais que le second facteur
 * reste à fournir. Il ne confère **aucun** droit d'accès : il ne sert qu'à corréler la seconde étape de la
 * connexion, et disparaît dès qu'il est consommé ou expiré.
 */
interface MfaChallengeRepository {
    /** Émet un challenge pour [userId], valable [ttl], et retourne le jeton opaque à transmettre au front. */
    fun issue(
        userId: UserId,
        ttl: Duration,
    ): String

    fun findUserId(token: String): UserId?

    /** Incrémente et retourne le nombre de tentatives infructueuses sur ce challenge. */
    fun incrementAttempts(token: String): Int

    fun delete(token: String)
}
