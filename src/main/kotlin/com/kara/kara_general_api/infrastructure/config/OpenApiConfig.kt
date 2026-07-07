package com.kara.kara_general_api.infrastructure.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val BEARER_AUTH_SCHEME = "bearerAuth"

@Configuration
class OpenApiConfig {

    @Bean
    fun karaOpenAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Kara API")
                    .description("Documentation de l'API Kara (authentification, gestion des utilisateurs).")
                    .version("0.0.1-SNAPSHOT"),
            )
            .components(
                Components().addSecuritySchemes(
                    BEARER_AUTH_SCHEME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )
}
