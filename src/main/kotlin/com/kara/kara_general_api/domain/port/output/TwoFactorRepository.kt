package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.twofactor.TwoFactorSecret

/** Persistance du secret TOTP (au plus un par compte). Le secret y transite toujours chiffré. */
interface TwoFactorRepository {
    fun findByUserId(userId: UserId): TwoFactorSecret?

    /** Insère ou remplace le secret du compte (un `PENDING` précédent est écrasé). */
    fun save(secret: TwoFactorSecret): TwoFactorSecret

    fun deleteByUserId(userId: UserId)

    /** Consomme un pas de temps TOTP (anti-rejeu). */
    fun updateLastUsedStep(
        userId: UserId,
        step: Long,
    )
}
