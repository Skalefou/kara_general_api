package com.kara.kara_general_api.domain.model.room.vo

data class Coordinates(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude invalide" }
        require(longitude in -180.0..180.0) { "Longitude invalide" }
    }
}
