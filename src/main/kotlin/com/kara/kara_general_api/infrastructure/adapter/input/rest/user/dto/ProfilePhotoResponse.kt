package com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ProfilePhotoResponse(
    @field:Schema(description = "URL signée courte durée vers la photo de profil")
    val photoUrl: String,
)
