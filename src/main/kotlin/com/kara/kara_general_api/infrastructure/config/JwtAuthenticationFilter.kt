package com.kara.kara_general_api.infrastructure.config

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.security.interfaces.RSAPublicKey

class JwtAuthenticationFilter(
    private val jwtPublicKey: RSAPublicKey,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.removePrefix("Bearer ")
            try {
                val claims =
                    Jwts.parser()
                        .verifyWith(jwtPublicKey)
                        .build()
                        .parseSignedClaims(token)
                        .payload

                val userId = claims.subject
                val role = claims["role"] as? String ?: ""
                val auth =
                    UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_$role")),
                    )
                SecurityContextHolder.getContext().authentication = auth
            } catch (_: JwtException) {
                SecurityContextHolder.clearContext()
            }
        }
        filterChain.doFilter(request, response)
    }
}
