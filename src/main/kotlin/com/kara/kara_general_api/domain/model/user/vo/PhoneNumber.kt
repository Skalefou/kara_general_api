package com.kara.kara_general_api.domain.model.user.vo

@JvmInline
value class PhoneNumber(
    val value: String,
) {
    init {
        require(PHONE_REGEX.matches(value)) { "Numéro de téléphone invalide" }
    }

    companion object {
        private val PHONE_REGEX = Regex("^\\+?[0-9]{8,15}$")
    }
}
