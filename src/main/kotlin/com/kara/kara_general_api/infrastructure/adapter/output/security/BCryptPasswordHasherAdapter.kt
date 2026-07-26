package com.kara.kara_general_api.infrastructure.adapter.output.security

import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptPasswordHasherAdapter(
    private val passwordEncoder: PasswordEncoder,
) : PasswordHasher {
    override fun hash(plainPassword: String): HashedPassword = HashedPassword(requireNotNull(passwordEncoder.encode(plainPassword)))

    override fun matches(
        plainPassword: String,
        hashedPassword: HashedPassword,
    ): Boolean = passwordEncoder.matches(plainPassword, hashedPassword.value)
}
