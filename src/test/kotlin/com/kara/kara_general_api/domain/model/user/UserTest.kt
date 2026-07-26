package com.kara.kara_general_api.domain.model.user

import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserTest {
    private val expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)

    private fun serverAccount(): User =
        User.createServerAccount(
            email = Email("server@kara.app"),
            hashedPassword = HashedPassword("hashed"),
            firstName = "Jane",
            lastName = "Doe",
            phoneNumber = PhoneNumber("0612345678"),
            birthDate = LocalDate.of(1990, 1, 15),
            firebaseUid = "firebase-uid",
            tempPasswordExpiresAt = expiresAt,
        )

    @Test
    fun `should create a server account with forced password change and verified email`() {
        val user = serverAccount()

        assertEquals(UserRole.SERVER, user.role)
        assertTrue(user.mustChangePassword)
        assertTrue(user.emailVerified)
        assertEquals(expiresAt, user.tempPasswordExpiresAt)
        assertNull(user.deactivatedAt)
    }

    @Test
    fun `should mark account as deactivated`() {
        val user = serverAccount().deactivate()

        assertNotNull(user.deactivatedAt)
    }

    @Test
    fun `should reset temporary password state when password is changed`() {
        val user = serverAccount().passwordChanged(HashedPassword("new-hash"))

        assertEquals(HashedPassword("new-hash"), user.hashedPassword)
        assertFalse(user.mustChangePassword)
        assertNull(user.tempPasswordExpiresAt)
    }

    @Test
    fun `should renew temporary password state when reinvited`() {
        val newExpiry = Instant.now().plus(24, ChronoUnit.HOURS)
        val user = serverAccount().passwordChanged(HashedPassword("old")).reinvited(HashedPassword("temp"), newExpiry)

        assertEquals(HashedPassword("temp"), user.hashedPassword)
        assertTrue(user.mustChangePassword)
        assertEquals(newExpiry, user.tempPasswordExpiresAt)
    }

    @Test
    fun `should report temporary password as expired when expiry is in the past`() {
        val user = serverAccount().reinvited(HashedPassword("temp"), Instant.now().minusSeconds(1))

        assertTrue(user.isTempPasswordExpired(Instant.now()))
    }

    @Test
    fun `should not report expiry when password change is not forced`() {
        val user = serverAccount().passwordChanged(HashedPassword("final"))

        assertFalse(user.isTempPasswordExpired(Instant.now()))
    }
}
