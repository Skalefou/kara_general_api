package com.kara.kara_general_api.domain.model.user.vo

@JvmInline
value class Email private constructor(
    val value: String,
) {
    init {
        require(EMAIL_REGEX.matches(value)) { "Email invalide" }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        operator fun invoke(raw: String): Email = Email(raw.trim().lowercase())
    }
}
