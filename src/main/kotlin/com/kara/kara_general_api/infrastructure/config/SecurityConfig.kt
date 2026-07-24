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
                // Stock par salle : géré par le serveur de service (autorisation fine dans le use case) ou
                // l'admin. Doit précéder les règles /rooms/** génériques (GET public, DELETE ADMIN) ci-dessous.
                authorize(HttpMethod.GET, "/api/v1/rooms/*/stock", hasAnyRole(UserRole.SERVER.name, UserRole.ADMIN.name))
                authorize(HttpMethod.PUT, "/api/v1/rooms/*/stock/**", hasAnyRole(UserRole.SERVER.name, UserRole.ADMIN.name))
                authorize(HttpMethod.DELETE, "/api/v1/rooms/*/stock/**", hasAnyRole(UserRole.SERVER.name, UserRole.ADMIN.name))
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
                // Réservations rattachées au serveur (via son agenda) : réservées au rôle SERVER.
                authorize(HttpMethod.GET, "/api/v1/bookings/me/assigned", hasRole(UserRole.SERVER.name))
                // Supervision admin : toutes les réservations et tous les chats.
                authorize(HttpMethod.GET, "/api/v1/bookings", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.GET, "/api/v1/chat/admin/**", hasRole(UserRole.ADMIN.name))
                // Création de réservation et initiation de paiement : réservées au client authentifié.
                authorize(HttpMethod.POST, "/api/v1/bookings", authenticated)
                authorize(HttpMethod.POST, "/api/v1/bookings/*/payments", authenticated)
                // Commande d'un produit pendant une réservation active : réservée au client authentifié.
                // L'autorisation propriétaire (réservation appartenant au client) est vérifiée dans le use case.
                authorize(HttpMethod.POST, "/api/v1/bookings/*/orders", authenticated)
                // Produits commandables d'une réservation : lecture réservée au client (propriété vérifiée
                // dans le use case).
                authorize(HttpMethod.GET, "/api/v1/bookings/*/available-products", authenticated)
                // Catalogue global des services : gestion réservée au back-office (ADMIN) pour toutes les
                // opérations (création, listing de gestion, suppression).
                authorize(HttpMethod.POST, "/api/v1/services", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.GET, "/api/v1/services", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.PATCH, "/api/v1/services/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.DELETE, "/api/v1/services/**", hasRole(UserRole.ADMIN.name))
                // Catalogue générique des produits consommables : création/modification/suppression réservées
                // au back-office (ADMIN). Lecture ouverte aussi au SERVER : il pioche dans le catalogue pour
                // garnir le stock de sa salle.
                authorize(HttpMethod.POST, "/api/v1/products", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.GET, "/api/v1/products", hasAnyRole(UserRole.SERVER.name, UserRole.ADMIN.name))
                authorize(HttpMethod.PATCH, "/api/v1/products/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.DELETE, "/api/v1/products/**", hasRole(UserRole.ADMIN.name))
                authorize("/api/v1/users/me", authenticated)
                authorize("/api/v1/users/me/**", authenticated)
                authorize("/api/v1/users", hasRole(UserRole.ADMIN.name))
                authorize("/api/v1/users/**", hasRole(UserRole.ADMIN.name))
                // Agenda personnel du serveur : un SERVER consulte ses propres créneaux (doit précéder
                // les règles ADMIN génériques ci-dessous, sinon /me tomberait sous /server-shifts/**).
                authorize(HttpMethod.GET, "/api/v1/server-shifts/me", hasRole(UserRole.SERVER.name))
                // Agenda des serveurs : édition réservée au back-office (ADMIN) sur toutes les opérations.
                authorize("/api/v1/server-shifts", hasRole(UserRole.ADMIN.name))
                authorize("/api/v1/server-shifts/**", hasRole(UserRole.ADMIN.name))
                // Récapitulatifs publics de cagnotte (lecture sans authentification). Le paiement d'une part
                // reste soumis à l'authentification (routes /shares/*/payment couvertes par anyRequest).
                authorize(HttpMethod.GET, "/api/v1/pools/join/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/pools/share/**", permitAll)
                // Observabilité (double barrière — cf. Caddyfile qui renvoie 404 sur /actuator* côté public,
                // et le pare-feu hôte qui n'expose jamais le port 8080 sur Internet).
                // Seuls ces trois endpoints actuator sont ouverts, et uniquement joignables via le réseau
                // Docker interne (sonde de santé du conteneur + scrape Prometheus) :
                //   - /actuator/health : liveness/readiness du conteneur ;
                //   - /actuator/prometheus : métriques Micrometer, scrapées par Prometheus (api:8080) ;
                //   - /actuator/info : métadonnées applicatives, sans secret.
                // Aucun autre endpoint actuator n'est exposé (management.endpoints.web.exposure.include).
                authorize(HttpMethod.GET, "/actuator/health", permitAll)
                authorize(HttpMethod.GET, "/actuator/health/**", permitAll)
                authorize(HttpMethod.GET, "/actuator/prometheus", permitAll)
                authorize(HttpMethod.GET, "/actuator/info", permitAll)
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