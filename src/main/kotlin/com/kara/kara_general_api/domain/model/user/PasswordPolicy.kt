package com.kara.kara_general_api.domain.model.user

object PasswordPolicy {

    fun validate(
        password: String,
        role: UserRole,
    ): List<String> =
        when (role) {
            UserRole.SERVER, UserRole.ADMIN -> validateStaffPassword(password)
            UserRole.CLIENT, UserRole.GUEST -> validateClientPassword(password)
        }

    private fun validateClientPassword(password: String): List<String> {
        val issues = mutableListOf<String>()
        if (password.length < 8) issues += "Le mot de passe doit contenir au moins 8 caractères"
        if (!password.any { it.isDigit() }) issues += "Le mot de passe doit contenir au moins un chiffre"
        if (!password.any { it.isLetter() }) issues += "Le mot de passe doit contenir au moins une lettre"
        if (!password.any { it.isUpperCase() }) issues += "Le mot de passe doit contenir au moins une majuscule"
        return issues
    }

    private fun validateStaffPassword(password: String): List<String> {
        val issues = mutableListOf<String>()
        if (password.length < 32) issues += "Le mot de passe doit contenir au moins 32 caractères"
        if (!password.any { it.isUpperCase() }) issues += "Le mot de passe doit contenir au moins une majuscule"
        if (!password.any { it.isLowerCase() }) issues += "Le mot de passe doit contenir au moins une minuscule"
        if (!password.any { it.isDigit() }) issues += "Le mot de passe doit contenir au moins un chiffre"
        if (password.none { !it.isLetterOrDigit() }) {
            issues += "Le mot de passe doit contenir au moins un caractère spécial"
        }
        return issues
    }
}
