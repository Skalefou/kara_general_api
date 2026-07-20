package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.port.input.pool.RegeneratePoolLinkCommand
import com.kara.kara_general_api.domain.port.input.pool.RegeneratePoolLinkResult
import com.kara.kara_general_api.domain.port.input.pool.RegeneratePoolLinkUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.LinkTokenGenerator
import com.kara.kara_general_api.domain.port.output.PoolRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Régénère le token de lien global d'une cagnotte (invalide l'ancien lien de partage). */
@Service
class RegeneratePoolLinkService(
    private val poolRepository: PoolRepository,
    private val bookingRepository: BookingRepository,
    private val linkTokenGenerator: LinkTokenGenerator,
) : RegeneratePoolLinkUseCase {

    @Transactional
    override fun regenerate(command: RegeneratePoolLinkCommand): RegeneratePoolLinkResult {
        val pool = poolRepository.findById(command.poolId) ?: return RegeneratePoolLinkResult.PoolNotFound
        val booking = bookingRepository.findById(pool.bookingId) ?: return RegeneratePoolLinkResult.PoolNotFound
        if (booking.userId != command.requesterId) return RegeneratePoolLinkResult.NotOwner
        if (!pool.isOpen()) return RegeneratePoolLinkResult.PoolClosed

        val token = linkTokenGenerator.generate()
        poolRepository.updateGlobalLinkToken(pool.id, token)
        return RegeneratePoolLinkResult.Regenerated(token)
    }
}
