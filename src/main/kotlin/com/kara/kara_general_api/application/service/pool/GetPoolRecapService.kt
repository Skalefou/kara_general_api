package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.pool.GetPoolRecapResult
import com.kara.kara_general_api.domain.port.input.pool.GetPoolRecapUseCase
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Récapitulatif public d'une cagnotte (lecture sans authentification).
 *
 * Sur le lien global, l'authentification est **facultative** : quand elle est présente, la part dont l'appelant
 * est le payeur est jointe au récapitulatif, de quoi reprendre un paiement interrompu sans avoir à recréer une
 * part (ce que la règle « une part par personne » interdit).
 */
@Service
class GetPoolRecapService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val poolRecapAssembler: PoolRecapAssembler,
) : GetPoolRecapUseCase {
    @Transactional(readOnly = true)
    override fun getByGlobalToken(
        globalToken: String,
        callerId: UserId?,
    ): GetPoolRecapResult {
        val pool = poolRepository.findByGlobalLinkToken(globalToken) ?: return GetPoolRecapResult.NotFound
        // Invité : aucune interrogation des parts. Authentifié : uniquement la part dont il est le payeur.
        val callerShare = callerId?.let { poolShareRepository.findByPoolIdAndPayerUserId(pool.id, it) }
        return recap(pool, share = callerShare)
    }

    @Transactional(readOnly = true)
    override fun getByShareToken(shareToken: String): GetPoolRecapResult {
        val share = poolShareRepository.findByUniqueLinkToken(shareToken) ?: return GetPoolRecapResult.NotFound
        val pool = poolRepository.findById(share.poolId) ?: return GetPoolRecapResult.NotFound
        return recap(pool, share = share)
    }

    private fun recap(
        pool: Pool,
        share: PoolShare?,
    ): GetPoolRecapResult =
        poolRecapAssembler.assemble(pool, share)?.let { GetPoolRecapResult.Found(it) }
            ?: GetPoolRecapResult.NotFound
}
