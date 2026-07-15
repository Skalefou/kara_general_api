package com.kara.kara_general_api.domain.model.room

import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.room.vo.Coordinates
import java.math.BigDecimal
import java.time.Instant

data class Room(
    val id: RoomId,
    val name: String,
    val description: String,
    val address: Address,
    val pricePerPersonPerHour: BigDecimal,
    val currency: Currency,
    val isThereWifi: Boolean,
    val isThereSonoPro: Boolean,
    val isThereAirConditioning: Boolean,
    val createdAt: Instant,
    val status: RoomStatus = RoomStatus.OPEN,
    val images: List<RoomImage> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    init {
        require(name.isNotBlank()) { "Le nom de la salle est obligatoire" }
        require(pricePerPersonPerHour >= BigDecimal.ZERO) { "Le prix par personne et par heure doit être positif" }
    }

    fun update(
        name: String,
        description: String,
        address: Address,
        pricePerPersonPerHour: BigDecimal,
        currency: Currency,
        isThereWifi: Boolean,
        isThereSonoPro: Boolean,
        isThereAirConditioning: Boolean,
        status: RoomStatus,
        coordinates: Coordinates?,
    ): Room =
        copy(
            name = name,
            description = description,
            address = address,
            pricePerPersonPerHour = pricePerPersonPerHour,
            currency = currency,
            isThereWifi = isThereWifi,
            isThereSonoPro = isThereSonoPro,
            isThereAirConditioning = isThereAirConditioning,
            status = status,
            latitude = coordinates?.latitude,
            longitude = coordinates?.longitude,
        )

    companion object {
        fun create(
            name: String,
            description: String,
            address: Address,
            pricePerPersonPerHour: BigDecimal,
            currency: Currency,
            isThereWifi: Boolean,
            isThereSonoPro: Boolean,
            isThereAirConditioning: Boolean,
            coordinates: Coordinates,
        ): Room =
            Room(
                id = RoomId.generate(),
                name = name,
                description = description,
                address = address,
                pricePerPersonPerHour = pricePerPersonPerHour,
                currency = currency,
                isThereWifi = isThereWifi,
                isThereSonoPro = isThereSonoPro,
                isThereAirConditioning = isThereAirConditioning,
                createdAt = Instant.now(),
                status = RoomStatus.OPEN,
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
            )
    }
}
