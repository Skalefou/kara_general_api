package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.UserRole

interface PasswordGenerator {
    /**
     * Génère un mot de passe aléatoire conforme à la politique du rôle donné
     * (cf. [com.kara.kara_general_api.domain.model.user.PasswordPolicy]).
     */
    fun generate(role: UserRole): String
}
