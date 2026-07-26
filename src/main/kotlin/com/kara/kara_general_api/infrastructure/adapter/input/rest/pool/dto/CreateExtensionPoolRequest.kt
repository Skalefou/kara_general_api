package com.kara.kara_general_api.infrastructure.adapter.input.rest.pool.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class CreateExtensionPoolRequest(
    @field:NotEmpty
    @field:Valid
    @field:Schema(description = "Parts de la cagnotte : leur somme doit égaler le prix de l'extension")
    val shares: List<CreatePoolShareRequest>,
)
