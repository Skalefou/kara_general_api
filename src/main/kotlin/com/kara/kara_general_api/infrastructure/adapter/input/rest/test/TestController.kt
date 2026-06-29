package com.kara.kara_general_api.infrastructure.adapter.input.rest.test

import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.output.EmailVerificationCodeRepository
import com.kara.kara_general_api.domain.port.output.PasswordResetCodeRepository
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/test")
@Profile("dev")
class TestController(
    private val emailVerificationCodeRepository: EmailVerificationCodeRepository,
    private val passwordResetCodeRepository: PasswordResetCodeRepository,
) {

    @GetMapping("/auth/verification-code")
    fun getVerificationCode(@RequestParam email: String): ResponseEntity<Map<String, String?>> {
        val code = emailVerificationCodeRepository.find(Email(email))
        return ResponseEntity.ok(mapOf("code" to code))
    }

    @GetMapping("/auth/reset-code")
    fun getPasswordResetCode(@RequestParam email: String): ResponseEntity<Map<String, String?>> {
        val code = passwordResetCodeRepository.find(Email(email))
        return ResponseEntity.ok(mapOf("code" to code))
    }
}
