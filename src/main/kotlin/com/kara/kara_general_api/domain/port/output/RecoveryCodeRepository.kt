package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCode
import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCodeId

/** Persistance des codes de secours. Seuls des hachés bcrypt y transitent, jamais un code en clair. */
interface RecoveryCodeRepository {
    /** Remplace l'intégralité des codes du compte par [codeHashes] (les anciens sont supprimés). */
    fun replaceAll(
        userId: UserId,
        codeHashes: List<String>,
    )

    fun findUnusedByUserId(userId: UserId): List<RecoveryCode>

    fun markUsed(id: RecoveryCodeId)

    fun deleteByUserId(userId: UserId)

    fun countUnused(userId: UserId): Int
}
