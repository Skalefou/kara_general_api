package com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description =
        "Codes de secours à usage unique. Affichés UNE SEULE FOIS : ils ne sont stockés que sous forme " +
            "hachée et ne pourront jamais être réaffichés.",
)
data class RecoveryCodesResponse(
    @field:Schema(
        description = "Liste des codes de secours en clair, composés de mots séparés par des espaces",
        example = "[\"cascade tulipe marteau renard\", \"biscuit orage colline banjo\"]",
    )
    val recoveryCodes: List<String>,
)
