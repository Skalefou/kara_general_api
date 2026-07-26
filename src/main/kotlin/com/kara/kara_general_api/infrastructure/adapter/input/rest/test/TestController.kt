package com.kara.kara_general_api.infrastructure.adapter.input.rest.test

import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.output.EmailVerificationCodeRepository
import com.kara.kara_general_api.domain.port.output.PasswordResetCodeRepository
import com.kara.kara_general_api.domain.port.output.SecretCipher
import com.kara.kara_general_api.domain.port.output.TotpService
import com.kara.kara_general_api.domain.port.output.TwoFactorRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.HashingAlgorithm
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
    private val userRepository: UserRepository,
    private val twoFactorRepository: TwoFactorRepository,
    private val secretCipher: SecretCipher,
    private val totpService: TotpService,
) {
    private val codeGenerator = DefaultCodeGenerator(HashingAlgorithm.SHA1)

    @GetMapping("/auth/verification-code")
    fun getVerificationCode(
        @RequestParam email: String,
    ): ResponseEntity<Map<String, String?>> {
        val code = emailVerificationCodeRepository.find(Email(email))
        return ResponseEntity.ok(mapOf("code" to code))
    }

    @GetMapping("/auth/reset-code")
    fun getPasswordResetCode(
        @RequestParam email: String,
    ): ResponseEntity<Map<String, String?>> {
        val code = passwordResetCodeRepository.find(Email(email))
        return ResponseEntity.ok(mapOf("code" to code))
    }

    /**
     * Code TOTP courant du compte, pour permettre les tests end-to-end du front sans piloter une véritable
     * application d'authentification. Profil `dev` uniquement : exposer cette route ailleurs annulerait
     * purement et simplement l'intérêt de l'A2F.
     */
    @GetMapping("/auth/totp-code")
    fun getCurrentTotpCode(
        @RequestParam email: String,
    ): ResponseEntity<Map<String, String?>> {
        val user = userRepository.findByEmail(Email(email)) ?: return ResponseEntity.notFound().build()
        val secret = twoFactorRepository.findByUserId(user.id) ?: return ResponseEntity.notFound().build()
        val code =
            codeGenerator.generate(secretCipher.decrypt(secret.secretCipher), totpService.currentStep())
        return ResponseEntity.ok(mapOf("code" to code))
    }
}
