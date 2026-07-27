package com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description =
        "Secret TOTP à enregistrer dans une application d'authentification (Aegis, Proton Authenticator, " +
            "Google Authenticator). Affiché une seule fois : l'activation doit être confirmée par un premier code.",
)
data class TwoFactorSetupResponse(
    @field:Schema(
        description = "Clé secrète en base32, à saisir manuellement si le QR code ne peut pas être scanné",
        example = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP",
    )
    val secret: String,
    @field:Schema(
        description = "URI otpauth:// à encoder dans le QR code",
        example = "otpauth://totp/Kara%3Aclient%40kara.app?secret=JBSWY3DPEHPK3PXP&issuer=Kara&algorithm=SHA1&digits=6&period=30",
    )
    val otpauthUri: String,
)
