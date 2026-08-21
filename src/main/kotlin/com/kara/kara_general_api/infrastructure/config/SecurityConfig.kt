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
                authorize("/ws/**", permitAll)
                authorize("/error", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/rooms/*/stock", hasAnyRole(UserRole.SERVER.name, UserRole.ADMIN.name))
                authorize(HttpMethod.PUT, "/api/v1/rooms/*/stock/**", hasAnyRole(UserRole.SERVER.name, UserRole.ADMIN.name))
                authorize(HttpMethod.DELETE, "/api/v1/rooms/*/stock/**", hasAnyRole(UserRole.SERVER.name, UserRole.ADMIN.name))
                authorize(HttpMethod.GET, "/api/v1/rooms/**", permitAll)
                authorize(HttpMethod.POST, "/api/v1/rooms", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.POST, "/api/v1/rooms/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.PATCH, "/api/v1/rooms/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.DELETE, "/api/v1/rooms/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.POST, "/api/v1/bookings/estimate", permitAll)
                authorize(HttpMethod.POST, "/api/v1/stripe/webhook", permitAll)
                authorize(HttpMethod.GET, "/api/v1/bookings/me/assigned", hasRole(UserRole.SERVER.name))
                authorize(HttpMethod.GET, "/api/v1/bookings/me", authenticated)
                authorize(
                    HttpMethod.POST,
                    "/api/v1/bookings/*/validate-access",
                    hasAnyRole(UserRole.SERVER.name, UserRole.ADMIN.name),
                )
                authorize(HttpMethod.GET, "/api/v1/bookings", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.GET, "/api/v1/chat/admin/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.POST, "/api/v1/bookings", authenticated)
                authorize(HttpMethod.POST, "/api/v1/bookings/*/payments", authenticated)
                authorize(HttpMethod.POST, "/api/v1/bookings/*/payments/*/sync", authenticated)
                authorize(HttpMethod.POST, "/api/v1/bookings/*/orders", authenticated)
                authorize(HttpMethod.GET, "/api/v1/bookings/*/available-products", authenticated)
                authorize(HttpMethod.GET, "/api/v1/bookings/*/extension-options", authenticated)
                authorize(HttpMethod.POST, "/api/v1/bookings/*/extensions", authenticated)
                authorize(HttpMethod.POST, "/api/v1/extensions/*/payment", authenticated)
                authorize(HttpMethod.POST, "/api/v1/extensions/*/pool", authenticated)
                authorize(HttpMethod.POST, "/api/v1/services", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.GET, "/api/v1/services", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.PATCH, "/api/v1/services/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.DELETE, "/api/v1/services/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.POST, "/api/v1/products", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.GET, "/api/v1/products", hasAnyRole(UserRole.SERVER.name, UserRole.ADMIN.name))
                authorize(HttpMethod.PATCH, "/api/v1/products/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.DELETE, "/api/v1/products/**", hasRole(UserRole.ADMIN.name))
                authorize("/api/v1/users/me", authenticated)
                authorize("/api/v1/users/me/**", authenticated)
                authorize("/api/v1/users", hasRole(UserRole.ADMIN.name))
                authorize("/api/v1/users/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.GET, "/api/v1/server-shifts/me", hasRole(UserRole.SERVER.name))
                authorize("/api/v1/server-shifts", hasRole(UserRole.ADMIN.name))
                authorize("/api/v1/server-shifts/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.GET, "/api/v1/pools/join/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/pools/share/**", permitAll)
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
