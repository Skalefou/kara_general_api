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
                authorize("/error", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/rooms/**", permitAll)
                authorize(HttpMethod.POST, "/api/v1/rooms", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.POST, "/api/v1/rooms/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.PATCH, "/api/v1/rooms/**", hasRole(UserRole.ADMIN.name))
                authorize(HttpMethod.DELETE, "/api/v1/rooms/**", hasRole(UserRole.ADMIN.name))
                authorize("/api/v1/users/me", authenticated)
                authorize("/api/v1/users/me/**", authenticated)
                authorize("/api/v1/users", hasRole(UserRole.ADMIN.name))
                authorize("/api/v1/users/**", hasRole(UserRole.ADMIN.name))
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