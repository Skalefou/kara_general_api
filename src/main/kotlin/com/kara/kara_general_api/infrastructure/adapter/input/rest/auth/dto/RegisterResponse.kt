package com.kara.kara_general_api.infrastructure.adapter.input.rest.auth.dto

import java.util.UUID

data class RegisterResponse(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
)
