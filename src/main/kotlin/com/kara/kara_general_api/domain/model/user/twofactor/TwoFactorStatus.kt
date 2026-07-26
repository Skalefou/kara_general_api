package com.kara.kara_general_api.domain.model.user.twofactor

/**
 * Cycle de vie du secret TOTP d'un compte.
 *
 * - [PENDING] : secret généré et affiché (QR code), mais l'utilisateur n'a pas encore prouvé que son
 *   application d'authentification est correctement configurée. L'A2F n'est PAS exigée à la connexion.
 * - [ACTIVE] : un premier code valide a été saisi. L'A2F est exigée à chaque connexion ultérieure.
 */
enum class TwoFactorStatus {
    PENDING,
    ACTIVE,
}
