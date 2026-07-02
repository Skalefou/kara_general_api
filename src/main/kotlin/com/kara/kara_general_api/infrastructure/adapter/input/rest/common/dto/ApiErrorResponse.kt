package com.kara.kara_general_api.infrastructure.adapter.input.rest.common.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Détail d'erreur (RFC 9457 Problem Details, enrichi d'un code applicatif)")
data class ApiErrorResponse(
    @field:Schema(description = "Titre court de l'erreur", example = "Compte introuvable")
    val title: String,
    @field:Schema(description = "Code de statut HTTP", example = "404")
    val status: Int,
    @field:Schema(description = "Description détaillée de l'erreur", example = "Aucun compte ne correspond à cet identifiant.")
    val detail: String,
    @field:Schema(description = "Chemin de la requête ayant échoué", example = "/api/v1/auth/login")
    val instance: String,
    @field:Schema(description = "Code d'erreur applicatif", example = "USER_NOT_FOUND")
    val code: String,
)
