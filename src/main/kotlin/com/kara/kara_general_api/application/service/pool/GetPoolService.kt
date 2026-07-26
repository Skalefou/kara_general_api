package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.pool.GetPoolResult
import com.kara.kara_general_api.domain.port.input.pool.GetPoolUseCase
import com.kara.kara_general_api.domain.port.input.pool.PoolView
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Statut complet d'une cagnotte, réservé au créateur (propriétaire de la réservation). Lecture seule. */
@Service
class GetPoolService(
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
    private val bookingRepository: BookingRepository,
) : GetPoolUseCase {
    @Transactional(readOnly = true)
    override fun getById(
        poolId: PoolId,
        requesterId: UserId,
    ): GetPoolResult {
        val pool = poolRepository.findById(poolId) ?: return GetPoolResult.NotFound
        return toResult(pool, requesterId)
    }

    @Transactional(readOnly = true)
    override fun getByBookingId(
        bookingId: BookingId,
        requesterId: UserId,
    ): GetPoolResult {
        val pool = poolRepository.findByBookingId(bookingId) ?: return GetPoolResult.NotFound
        return toResult(pool, requesterId)
    }

    @Transactional(readOnly = true)
    override fun getByExtensionId(
        extensionId: BookingExtensionId,
        requesterId: UserId,
    ): GetPoolResult {
        val pool = poolRepository.findByExtensionId(extensionId) ?: return GetPoolResult.NotFound
        return toResult(pool, requesterId)
    }

    private fun toResult(
        pool: Pool,
        requesterId: UserId,
    ): GetPoolResult {
        val booking = bookingRepository.findById(pool.bookingId) ?: return GetPoolResult.NotFound
        if (booking.userId != requesterId) return GetPoolResult.NotOwner
        val shares = poolShareRepository.findByPoolId(pool.id)
        return GetPoolResult.Found(PoolView.of(pool, shares))
    }
}
