package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.booking.UserBooking
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.user.UserId
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
 * Deux rôles y figurent : l'organisateur de la réservation et le participant qui a payé une part de sa
 * cagnotte (`isCreator` les distingue). Aucun statut n'est filtré : le front étiquette et regroupe. Le
 * nombre de requêtes est constant quel que soit le nombre de réservations : le repository en émet deux
 * (réservations + options de toutes les réservations), puis une requête charge les cagnottes de toutes les
 * réservations et une dernière leurs parts. Sans réservation, aucune requête de cagnotte n'est émise.
 *
 * **PII** : la liste nominative des parts (nom + email de chaque participant) n'est exposée qu'à
 * l'organisateur. Un participant non organisateur ne reçoit que la progression de la cagnotte et ses propres
 * parts, sans email — soit exactement ce que le récapitulatif de cagnotte (`GET /api/v1/pools/join/{token}`)
 * lui montre déjà, le détail de cagnotte `GET /api/v1/pools/{id}` lui étant refusé (POOL_NOT_OWNER).
 */
@Service
class ListUserBookingsService(
    private val bookingRepository: BookingRepository,
    private val poolRepository: PoolRepository,
    private val poolShareRepository: PoolShareRepository,
) : ListUserBookingsUseCase {
    @Transactional(readOnly = true)
    override fun listForUser(command: ListUserBookingsCommand): ListUserBookingsResult {
        val records = bookingRepository.findByUserInvolvement(command.userId)
        if (records.isEmpty()) return ListUserBookingsResult.Success(emptyList())

        val poolsByBooking = poolRepository.findByBookingIds(records.map { it.booking.id }).associateBy { it.bookingId }
        val sharesByPool =
            poolShareRepository.findByPoolIds(poolsByBooking.values.map { it.id }).groupBy { it.poolId }

        return ListUserBookingsResult.Success(
            records
                .sortedByDescending { it.booking.startAt }
                .map { record ->
                    val pool = poolsByBooking[record.booking.id]
                    toView(record, pool, pool?.let { sharesByPool[it.id] }.orEmpty(), command.userId)
                },
        )
    }

    private fun toView(
        record: UserBooking,
        pool: Pool?,
        shares: List<PoolShare>,
        requesterId: UserId,
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
            pool = pool?.let { toPoolView(it, shares, record.isCreator, requesterId) },
            isCreator = record.isCreator,
        )
    }

    /**
     * La progression de la cagnotte est calculée sur **toutes** les parts (le montant collecté est une
     * information de la cagnotte, pas d'un participant) ; seule la liste exposée est réduite selon le rôle.
     */
    private fun toPoolView(
        pool: Pool,
        shares: List<PoolShare>,
        isCreator: Boolean,
        requesterId: UserId,
    ): UserBookingPoolView {
        val collected = collectedAmount(shares)
        val visibleShares = if (isCreator) shares else shares.filter { it.payerUserId == requesterId }
        return UserBookingPoolView(
            poolId = pool.id.value,
            status = pool.status,
            targetAmount = pool.targetAmount,
            collectedAmount = collected,
            currency = pool.currency,
            percentage = percentage(collected, pool.targetAmount),
            deadline = pool.deadline,
            shares = visibleShares.map { toShareView(it, isCreator) },
        )
    }

    /** L'email d'une part n'est exposé qu'à l'organisateur : le récapitulatif de cagnotte n'en montre aucun. */
    private fun toShareView(
        share: PoolShare,
        isCreator: Boolean,
    ): UserBookingPoolShareView =
        UserBookingPoolShareView(
            shareId = share.id.value,
            participantName = share.participantName,
            email = share.email?.value.takeIf { isCreator },
            amount = share.amount,
            status = share.status,
        )

    private fun formatAddress(address: Address): String = with(address) { "$street, $postalCode $city, $country" }
}
