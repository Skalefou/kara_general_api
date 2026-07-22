package com.kara.kara_general_api.infrastructure.config

import com.kara.kara_general_api.domain.model.user.UserRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig {

    @Autowired(required = false)
    private var jwtAuthenticationFilter: JwtAuthenticationFilter? = null

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            httpBasic { disable() }
            formLogin { disable() }
            authorizeHttpRequests {
                authorize(HttpMethod.POST, "/api/v1/auth/change-password", authenticated)
                authorize("/api/v1/auth/**", permitAll)
                authorize("/api/v1/test/**", permitAll)
                // Handshake WebSocket : non authentifié ; l'authentification se fait sur la frame STOMP CONNECT.
                authorize("/ws/**", permitAll)
                authorize("/error", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/rooms/**", permitAll)
                authorize(HttpMethod.POST, "/api/v1/rooms", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.POST, "/api/v1/rooms/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.PATCH, "/api/v1/rooms/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.DELETE, "/api/v1/rooms/**", hasRole(UserRole.ADMIN.name))
                // Estimation tarifaire : lecture seule, aucune persistance ; accessible aux invités et clients
                // (même logique que la consultation publique des salles).
                authorize(HttpMethod.POST, "/api/v1/bookings/estimate", permitAll)
                // Webhook Stripe : signé et vérifié applicativement (STRIPE_WEBHOOK_SECRET), donc ouvert.
                authorize(HttpMethod.POST, "/api/v1/stripe/webhook", permitAll)
                // Création de réservation et initiation de paiement : réservées au client authentifié.
                authorize(HttpMethod.POST, "/api/v1/bookings", authenticated)
                authorize(HttpMethod.POST, "/api/v1/bookings/*/payments", authenticated)
                // Catalogue global des services : gestion réservée au back-office (ADMIN) pour toutes les
                // opérations (création, listing de gestion, suppression).
                authorize(HttpMethod.POST, "/api/v1/services", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.GET, "/api/v1/services", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.PATCH, "/api/v1/services/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.DELETE, "/api/v1/services/**", hasRole(UserRole.ADMIN.name))
                authorize("/api/v1/users/me", authenticated)
                authorize("/api/v1/users/me/**", authenticated)
                authorize("/api/v1/users", hasRole(UserRole.ADMIN.name))
                authorize("/api/v1/users/**", hasRole(UserRole.ADMIN.name))
                // Récapitulatifs publics de cagnotte (lecture sans authentification). Le paiement d'une part
                // reste soumis à l'authentification (routes /shares/*/payment couvertes par anyRequest).
                authorize(HttpMethod.GET, "/api/v1/pools/join/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/pools/share/**", permitAll)
                // Sonde de santé (liveness/readiness du conteneur Docker) : lecture publique
                // du seul endpoint /actuator/health, sans exposer les autres endpoints actuator.
                authorize(HttpMethod.GET, "/actuator/health", permitAll)
                authorize(anyRequest, authenticated)
            }
            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            }
        }
        jwtAuthenticationFilter?.let {
            http.addFilterBefore(it, UsernamePasswordAuthenticationFilter::class.java)
        }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)
}