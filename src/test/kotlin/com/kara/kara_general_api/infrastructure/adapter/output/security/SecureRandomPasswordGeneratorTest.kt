package com.kara.kara_general_api.infrastructure.adapter.output.security

import com.kara.kara_general_api.domain.model.user.PasswordPolicy
import com.kara.kara_general_api.domain.model.user.UserRole
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SecureRandomPasswordGeneratorTest {
    private val sut = SecureRandomPasswordGenerator()

    @Test
    fun `should generate a staff password satisfying the staff policy`() {
        repeat(50) {
            val password = sut.generate(UserRole.SERVER)

            assertTrue(password.length >= 32)
            assertEquals(emptyList(), PasswordPolicy.validate(password, UserRole.SERVER))
        }
    }

    @Test
    fun `should generate a client password satisfying the client policy`() {
        repeat(50) {
            val password = sut.generate(UserRole.CLIENT)

            assertEquals(emptyList(), PasswordPolicy.validate(password, UserRole.CLIENT))
        }
    }
}
