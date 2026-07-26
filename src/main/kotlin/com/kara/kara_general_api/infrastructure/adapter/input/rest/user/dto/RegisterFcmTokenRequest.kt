package com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class RegisterFcmTokenRequest(
    @field:NotBlank
    @field:Schema(description = "Token d'appareil FCM", example = "fMEP0v...:APA91b...")
    val token: String,
)
