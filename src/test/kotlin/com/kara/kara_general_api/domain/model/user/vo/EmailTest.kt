package com.kara.kara_general_api.domain.model.user.vo

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EmailTest {

    @Test
    fun `should create email when format is valid`() {
        val email = Email("client@kara.app")

        assertEquals("client@kara.app", email.value)
    }

    @Test
    fun `should throw when format is invalid`() {
        assertFailsWith<IllegalArgumentException> { Email("not-an-email") }
    }

    @Test
    fun `should normalize email to lowercase and trimmed`() {
        val email = Email("  Client@Kara.App  ")

        assertEquals("client@kara.app", email.value)
    }
}
