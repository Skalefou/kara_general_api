package com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "État de l'authentification à deux facteurs du compte")
data class TwoFactorStatusResponse(
    @field:Schema(description = "Vrai si l'A2F est active : un code est exigé à chaque connexion", example = "true")
    val enabled: Boolean,
    @field:Schema(
        description = "Vrai si un secret a été généré mais jamais confirmé par un premier code valide",
        example = "false",
    )
    val pendingSetup: Boolean,
    @field:Schema(description = "Nombre de codes de secours encore utilisables", example = "10")
    val remainingRecoveryCodes: Int,
)
