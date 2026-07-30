package com.kara.kara_general_api.infrastructure.adapter.input.rest.chat.dto

import io.swagger.v3.oas.annotations.media.Schema

data class RenameConversationRequest(
    @field:Schema(
        description = "Titre du groupe. Vide ou absent : restaure le titre déduit (salle et créneau, ou noms des participants).",
        example = "Anniversaire de Bruno",
    )
    val title: String? = null,
)
