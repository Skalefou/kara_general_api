package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.UserBooking
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.booking.ListUserBookingsCommand
import com.kara.kara_general_api.domain.port.input.booking.ListUserBookingsResult
import com.kara.kara_general_api.domain.port.input.booking.ListUserBookingsUseCase
import com.kara.kara_general_api.domain.port.input.booking.UserBookingOptionView
import com.kara.kara_general_api.domain.port.input.booking.UserBookingPoolShareView
import com.kara.kara_general_api.domain.port.input.booking.UserBookingPoolView
import com.kara.kara_general_api.domain.port.input.booking.UserBookingView
import com.kara.kara_general_api.domain.port.input.pool.collectedAmount
import com.kara.kara_general_api.domain.port.input.pool.percentage
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Réservations de l'utilisateur authentifié pour l'écran « Mes événements ». Lecture seule.
 *
 * Aucun statut n'est filtré : le front étiquette et regroupe. Le nombre de requêtes est constant quel que
 * soit le nombre de réservations : le repository en émet deux (réservations + options de toutes les
 * réservations), puis une requête charge les cagnottes de toutes les réservations et une dernière leurs
 * parts. Sans réservation, aucune requête de cagnotte n'est émise.
 */
@Service
class ListUserBookingsService(
    private val bookingRepository: BookingRepository,
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
) : ListUserBookingsUseCase {
    @Transactional(readOnly = true)
    override fun listForUser(command: ListUserBookingsCommand): ListUserBookingsResult {
        val records = bookingRepository.findByUserId(command.userId)
        if (records.isEmpty()) return ListUserBookingsResult.Success(emptyList())

        val poolsByBooking = poolRepository.findByBookingIds(records.map { it.booking.id }).associateBy { it.bookingId }
        val sharesByPool =
            poolShareRepository.findByPoolIds(poolsByBooking.values.map { it.id }).groupBy { it.poolId }

        return ListUserBookingsResult.Success(
            records
                .sortedByDescending { it.booking.startAt }
                .map { record ->
                    val pool = poolsByBooking[record.booking.id]
                    toView(record, pool, pool?.let { sharesByPool[it.id] }.orEmpty())
                },
        )
    }

    private fun toView(
        record: UserBooking,
        pool: Pool?,
        shares: List<PoolShare>,
    ): UserBookingView {
        val booking = record.booking
        return UserBookingView(
            bookingId = booking.id.value,
            roomId = booking.roomId.value,
            roomName = record.roomName,
            roomAddress = record.roomAddress?.let { formatAddress(it) },
            startAt = booking.startAt,
            endAt = booking.endAt,
            status = booking.status,
            paymentMode = booking.paymentMode,
            numberOfPeople = booking.numberOfPeople,
            totalPrice = booking.totalPrice,
            currency = booking.currency,
            expiresAt = booking.expiresAt,
            options =
                record.options.map { option ->
                    UserBookingOptionView(
                        optionId = option.optionId.value,
                        label = option.label,
                        price = option.price,
                        currency = option.currency,
                    )
                },
            pool = pool?.let { toPoolView(it, shares) },
        )
    }

    private fun toPoolView(
        pool: Pool,
        shares: List<PoolShare>,
    ): UserBookingPoolView {
        val collected = collectedAmount(shares)
        return UserBookingPoolView(
            poolId = pool.id.value,
            status = pool.status,
            targetAmount = pool.targetAmount,
            collectedAmount = collected,
            currency = pool.currency,
            percentage = percentage(collected, pool.targetAmount),
            deadline = pool.deadline,
            shares = shares.map { toShareView(it) },
        )
    }

    private fun toShareView(share: PoolShare): UserBookingPoolShareView =
        UserBookingPoolShareView(
            shareId = share.id.value,
            participantName = share.participantName,
            email = share.email?.value,
            amount = share.amount,
            status = share.status,
        )

    private fun formatAddress(address: Address): String = with(address) { "$street, $postalCode $city, $country" }
}
