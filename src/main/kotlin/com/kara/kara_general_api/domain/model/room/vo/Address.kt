package com.kara.kara_general_api.domain.model.room.vo

data class Address(
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String,
) {
    init {
        require(street.isNotBlank()) { "L'adresse est obligatoire" }
        require(city.isNotBlank()) { "La ville est obligatoire" }
        require(postalCode.isNotBlank()) { "Le code postal est obligatoire" }
        require(country.isNotBlank()) { "Le pays est obligatoire" }
    }
}
