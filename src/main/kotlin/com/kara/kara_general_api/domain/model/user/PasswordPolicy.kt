package com.kara.kara_general_api.domain.model.user

object PasswordPolicy {
    fun validate(password: String): List<String> {
        val issues = mutableListOf<String>()
        if (password.length < 8) issues += "Le mot de passe doit contenir au moins 8 caractères"
        if (!password.any { it.isDigit() }) issues += "Le mot de passe doit contenir au moins un chiffre"
        if (!password.any { it.isLetter() }) issues += "Le mot de passe doit contenir au moins une lettre"
        return issues
    }
}