package com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor

import com.kara.kara_general_api.infrastructure.adapter.input.rest.common.dto.ApiErrorResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.DisableTwoFactorRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.RecoveryCodesResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.RegenerateRecoveryCodesRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.TwoFactorActivateRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.TwoFactorSetupRequest
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.TwoFactorSetupResponse
import com.kara.kara_general_api.infrastructure.adapter.input.rest.twofactor.dto.TwoFactorStatusResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

private const val USER_NOT_FOUND_EXAMPLE = """
    {
      "title": "Compte introuvable",
      "status": 404,
      "detail": "Aucun compte ne correspond à cet identifiant.",
      "instance": "/api/v1/users/me/two-factor",
      "code": "USER_NOT_FOUND"
    }
"""

private const val INVALID_CURRENT_PASSWORD_EXAMPLE = """
    {
      "title": "Mot de passe incorrect",
      "status": 403,
      "detail": "Le mot de passe actuel est incorrect.",
      "instance": "/api/v1/users/me/two-factor",
      "code": "INVALID_CURRENT_PASSWORD"
    }
"""

private const val INVALID_TWO_FACTOR_CODE_EXAMPLE = """
    {
      "title": "Code invalide",
      "status": 400,
      "detail": "Le code fourni est incorrect.",
      "instance": "/api/v1/users/me/two-factor",
      "code": "INVALID_TWO_FACTOR_CODE"
    }
"""

private const val TWO_FACTOR_NOT_ENABLED_EXAMPLE = """
    {
      "title": "Authentification forte inactive",
      "status": 409,
      "detail": "L'authentification à deux facteurs n'est pas activée sur ce compte.",
      "instance": "/api/v1/users/me/two-factor",
      "code": "TWO_FACTOR_NOT_ENABLED"
    }
"""

@Tag(
    name = "Authentification à deux facteurs",
    description =
        "Mise en place, vérification et suppression de l'A2F TOTP (RFC 6238) du compte authentifié. " +
            "Compatible Aegis, Proton Authenticator et Google Authenticator.",
)
interface TwoFactorApi {
    @Operation(
        summary = "Consulter l'état de l'A2F du compte",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "État courant de l'A2F",
                content = [Content(schema = Schema(implementation = TwoFactorStatusResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [ExampleObject(name = "USER_NOT_FOUND", value = USER_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    @GetMapping
    fun status(authentication: Authentication): ResponseEntity<Any>

    @Operation(
        summary = "Préparer l'activation de l'A2F",
        description =
            "Génère un secret TOTP et retourne le QR code (URI `otpauth://`) ainsi que la clé secrète en " +
                "clair pour une saisie manuelle. L'A2F n'est PAS encore active : elle doit être confirmée par " +
                "POST /api/v1/users/me/two-factor/activate. Rejouer cette route écrase le secret non confirmé.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Secret généré, en attente de confirmation",
                content = [Content(schema = Schema(implementation = TwoFactorSetupResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Mot de passe actuel incorrect",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "INVALID_CURRENT_PASSWORD",
                                value = INVALID_CURRENT_PASSWORD_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [ExampleObject(name = "USER_NOT_FOUND", value = USER_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "L'A2F est déjà active : la désactiver d'abord",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "TWO_FACTOR_ALREADY_ENABLED",
                                value = """
                                    {
                                      "title": "Authentification forte déjà active",
                                      "status": 409,
                                      "detail": "L'authentification à deux facteurs est déjà activée sur ce compte.",
                                      "instance": "/api/v1/users/me/two-factor/setup",
                                      "code": "TWO_FACTOR_ALREADY_ENABLED"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/setup")
    fun setup(
        @Valid @RequestBody request: TwoFactorSetupRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Confirmer l'activation de l'A2F et récupérer les codes de secours",
        description =
            "Valide un premier code à usage unique. En cas de succès, l'A2F devient active, un email de " +
                "notification est envoyé et les codes de secours sont retournés — **seule et unique fois où " +
                "ils sont affichés**.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "A2F activée, codes de secours délivrés",
                content = [Content(schema = Schema(implementation = RecoveryCodesResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Code à usage unique incorrect",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "INVALID_TWO_FACTOR_CODE",
                                value = INVALID_TWO_FACTOR_CODE_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [ExampleObject(name = "USER_NOT_FOUND", value = USER_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Aucune configuration en attente, ou A2F déjà active",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "TWO_FACTOR_SETUP_NOT_FOUND",
                                value = """
                                    {
                                      "title": "Aucune configuration en attente",
                                      "status": 409,
                                      "detail": "Aucune configuration d'authentification forte n'est en attente. Recommencez la mise en place.",
                                      "instance": "/api/v1/users/me/two-factor/activate",
                                      "code": "TWO_FACTOR_SETUP_NOT_FOUND"
                                    }
                                """,
                            ),
                            ExampleObject(
                                name = "TWO_FACTOR_ALREADY_ENABLED",
                                value = """
                                    {
                                      "title": "Authentification forte déjà active",
                                      "status": 409,
                                      "detail": "L'authentification à deux facteurs est déjà activée sur ce compte.",
                                      "instance": "/api/v1/users/me/two-factor/activate",
                                      "code": "TWO_FACTOR_ALREADY_ENABLED"
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/activate")
    fun activate(
        @Valid @RequestBody request: TwoFactorActivateRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Régénérer les codes de secours",
        description =
            "Invalide tous les codes de secours existants et en délivre une nouvelle série. Exige le mot de " +
                "passe **et** un code à usage unique courant (un code de secours n'est pas accepté ici).",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Nouvelle série de codes de secours délivrée",
                content = [Content(schema = Schema(implementation = RecoveryCodesResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Code à usage unique incorrect",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "INVALID_TWO_FACTOR_CODE",
                                value = INVALID_TWO_FACTOR_CODE_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Mot de passe actuel incorrect",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "INVALID_CURRENT_PASSWORD",
                                value = INVALID_CURRENT_PASSWORD_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [ExampleObject(name = "USER_NOT_FOUND", value = USER_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "L'A2F n'est pas active sur ce compte",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "TWO_FACTOR_NOT_ENABLED",
                                value = TWO_FACTOR_NOT_ENABLED_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/recovery-codes")
    fun regenerateRecoveryCodes(
        @Valid @RequestBody request: RegenerateRecoveryCodesRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>

    @Operation(
        summary = "Désactiver l'A2F",
        description =
            "Invalide immédiatement la clé secrète et l'ensemble des codes de secours restants, puis envoie " +
                "un email de notification. Exige le mot de passe **et** un second facteur (code à usage " +
                "unique courant ou code de secours non consommé). Le compte redevient protégé par le seul " +
                "mot de passe.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "A2F désactivée"),
            ApiResponse(
                responseCode = "400",
                description = "Code (TOTP ou de secours) incorrect",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "INVALID_TWO_FACTOR_CODE",
                                value = INVALID_TWO_FACTOR_CODE_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Mot de passe actuel incorrect",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "INVALID_CURRENT_PASSWORD",
                                value = INVALID_CURRENT_PASSWORD_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Compte introuvable",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [ExampleObject(name = "USER_NOT_FOUND", value = USER_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "L'A2F n'est pas active sur ce compte",
                content = [
                    Content(
                        schema = Schema(implementation = ApiErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "TWO_FACTOR_NOT_ENABLED",
                                value = TWO_FACTOR_NOT_ENABLED_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @DeleteMapping
    fun disable(
        @Valid @RequestBody request: DisableTwoFactorRequest,
        authentication: Authentication,
    ): ResponseEntity<Any>
}
