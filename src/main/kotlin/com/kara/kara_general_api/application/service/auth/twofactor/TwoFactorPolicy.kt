package com.kara.kara_general_api.application.service.auth.twofactor

/** Paramètres partagés par les services A2F, pour qu'une seule valeur fasse foi. */
internal object TwoFactorPolicy {
    /** Nombre de codes de secours délivrés d'un coup (à l'activation comme à la régénération). */
    const val RECOVERY_CODE_COUNT: Int = 10

    /** Nombre de mots composant un code de secours. */
    const val RECOVERY_CODE_WORDS: Int = 4

    /** Tentatives infructueuses tolérées sur un challenge A2F avant destruction de celui-ci. */
    const val MAX_CHALLENGE_ATTEMPTS: Int = 5
}
