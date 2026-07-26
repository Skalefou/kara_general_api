package com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto

import com.kara.kara_general_api.domain.model.user.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

data class UserResponse(
    @field:Schema(description = "Identifiant unique de l'utilisateur")
    val id: UUID,
    @field:Schema(description = "Adresse email de l'utilisateur", example = "jane.doe@example.com")
    val email: String,
    @field:Schema(description = "Prénom de l'utilisateur", example = "Jane")
    val firstName: String,
    @field:Schema(description = "Nom de famille de l'utilisateur", example = "Doe")
    val lastName: String,
    @field:Schema(description = "Numéro de téléphone de l'utilisateur", example = "+33612345678")
    val phoneNumber: String,
    @field:Schema(description = "Date de naissance de l'utilisateur", example = "1990-01-15")
    val birthDate: LocalDate,
    @field:Schema(description = "Rôle de l'utilisateur", example = "CLIENT")
    val role: String,
    @field:Schema(description = "Indique si l'email a été vérifié")
    val emailVerified: Boolean,
    @field:Schema(
        description =
            "URL signée courte durée vers la variante `full` de la photo de profil " +
                "(null tant que le traitement n'est pas terminé ou si absente)",
    )
    val photoUrl: String?,
) {
    companion object {
        /**
         * [photoUrl] résout une clé d'objet en URL signée, appliquée à la variante `full` traitée (READY).
         * Omis (null) dans les contextes qui n'exposent pas la photo (ex. réponses d'authentification) ou tant
         * que les variantes ne sont pas prêtes.
         */
        fun from(
            user: User,
            photoUrl: ((String) -> String)? = null,
        ): UserResponse =
            UserResponse(
                id = user.id.value,
                email = user.email.value,
                firstName = user.firstName,
                lastName = user.lastName,
                phoneNumber = user.phoneNumber.value,
                birthDate = user.birthDate,
                role = user.role.name,
                emailVerified = user.emailVerified,
                photoUrl = user.photoFullKey?.let { key -> photoUrl?.invoke(key) },
            )
    }
}
