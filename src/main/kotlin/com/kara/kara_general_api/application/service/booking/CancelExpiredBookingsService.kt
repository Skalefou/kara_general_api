package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.port.input.booking.CancelExpiredBookingsUseCase
import com.kara.kara_general_api.domain.port.output.BookingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Annule en masse les réservations PENDING dont la fenêtre de paiement est échue. La sélection et la
 * mise à jour sont déléguées au [BookingRepository] (une seule requête SQL).
 */
@Service
class CancelExpiredBookingsService(
    private val bookingRepository: BookingRepository,
) : CancelExpiredBookingsUseCase {

    @Transactional
    override fun cancelExpired(now: Instant): Int = bookingRepository.cancelExpiredPending(now)
}
