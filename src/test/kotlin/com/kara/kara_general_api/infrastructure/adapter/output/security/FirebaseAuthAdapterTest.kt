package com.kara.kara_general_api.infrastructure.adapter.output.security

import com.google.firebase.ErrorCode
import com.google.firebase.auth.AuthErrorCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.output.EmailAlreadyUsedException
import com.kara.kara_general_api.domain.port.output.FirebaseUserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FirebaseAuthAdapterTest {

    private val firebaseAuth = mockk<FirebaseAuth>()
    private val sut = FirebaseAuthAdapter(firebaseAuth)

    private val email = Email("client@kara.app")
    private val plainPassword = "Azerty123"

    @Test
    fun `should return firebase user id when creation succeeds`() {
        val userRecord = mockk<UserRecord>()
        every { userRecord.uid } returns "firebase-uid"
        every { firebaseAuth.createUser(any()) } returns userRecord

        val result = sut.createUser(email, plainPassword)

        assertEquals(FirebaseUserId("firebase-uid"), result)
    }

    @Test
    fun `should throw EmailAlreadyUsedException when firebase reports email already exists`() {
        every { firebaseAuth.createUser(any()) } throws
            FirebaseAuthException(ErrorCode.ALREADY_EXISTS, "email exists", null, null, AuthErrorCode.EMAIL_ALREADY_EXISTS)

        assertFailsWith<EmailAlreadyUsedException> { sut.createUser(email, plainPassword) }
    }

    @Test
    fun `should recover firebase user id when creation throws but account already exists remotely`() {
        every { firebaseAuth.createUser(any()) } throws
            FirebaseAuthException(ErrorCode.UNKNOWN, "Not in GZIP format", null, null, null)
        val recoveredUserRecord = mockk<UserRecord>()
        every { recoveredUserRecord.uid } returns "firebase-uid"
        every { firebaseAuth.getUserByEmail(email.value) } returns recoveredUserRecord

        val result = sut.createUser(email, plainPassword)

        assertEquals(FirebaseUserId("firebase-uid"), result)
    }

    @Test
    fun `should rethrow original exception when account does not exist remotely either`() {
        val originalException = FirebaseAuthException(ErrorCode.UNKNOWN, "Not in GZIP format", null, null, null)
        every { firebaseAuth.createUser(any()) } throws originalException
        every { firebaseAuth.getUserByEmail(email.value) } throws
            FirebaseAuthException(ErrorCode.NOT_FOUND, "no user record", null, null, AuthErrorCode.USER_NOT_FOUND)

        val thrown = assertFailsWith<FirebaseAuthException> { sut.createUser(email, plainPassword) }

        assertEquals(originalException, thrown)
    }

    @Test
    fun `should delegate deleteUser to firebase auth`() {
        every { firebaseAuth.deleteUser("firebase-uid") } returns Unit

        sut.deleteUser(FirebaseUserId("firebase-uid"))

        verify { firebaseAuth.deleteUser("firebase-uid") }
    }
}