package com.kara.kara_general_api.domain.port.input.pool

import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.user.UserId

sealed interface GetPoolResult {
    data class Found(
        val view: PoolView,
    ) : GetPoolResult

    data object NotFound : GetPoolResult

    data object NotOwner : GetPoolResult
}

/** Statut complet d'une cagnotte, réservé au créateur (propriétaire de la réservation). */
interface GetPoolUseCase {
    fun getById(
        poolId: PoolId,
        requesterId: UserId,
    ): GetPoolResult

    fun getByBookingId(
        bookingId: BookingId,
        requesterId: UserId,
    ): GetPoolResult

    fun getByExtensionId(
        extensionId: BookingExtensionId,
        requesterId: UserId,
    ): GetPoolResult
}
