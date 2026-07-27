package com.kara.kara_general_api.domain.model.booking

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import com.kara.kara_general_api.domain.model.room.vo.Address
import java.math.BigDecimal

/**
 * Option (service) retenue par une réservation, avec le libellé et le prix forfaitaire du catalogue.
 * Le prix est un forfait fixe : il ne dépend ni du nombre de personnes ni de la durée.
 */
data class UserBookingOption(
    val optionId: RoomOptionId,
    val label: String,
    val price: BigDecimal,
    val currency: Currency,
)

/**
 * Vue d'une réservation destinée à son **propriétaire** (« Mes événements ») : la réservation, la salle
 * (nom + adresse) et les services retenus. L'éventuelle cagnotte n'est pas portée ici : elle est
 * assemblée par le service applicatif, qui la charge en une seule requête pour toutes les réservations.
 */
data class UserBooking(
    val booking: Booking,
    val roomName: String,
    val roomAddress: Address?,
    val options: List<UserBookingOption>,
)
