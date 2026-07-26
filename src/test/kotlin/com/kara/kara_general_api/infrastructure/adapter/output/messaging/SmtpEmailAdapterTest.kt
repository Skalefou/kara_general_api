package com.kara.kara_general_api.infrastructure.adapter.output.messaging

import com.kara.kara_general_api.domain.model.user.vo.Email
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Test
import org.springframework.mail.javamail.JavaMailSender
import java.util.Properties

class SmtpEmailAdapterTest {
    private val mailSender = mockk<JavaMailSender>()
    private val sut = SmtpEmailAdapter(mailSender, "no-reply@kara.app")

    @Test
    fun `should send a mime message with the verification code to the recipient`() {
        val message = MimeMessage(Session.getDefaultInstance(Properties()))
        every { mailSender.createMimeMessage() } returns message
        every { mailSender.send(message) } returns Unit

        sut.sendVerificationCode(Email("client@kara.app"), "123456")

        verify { mailSender.send(message) }
        kotlin.test.assertEquals("client@kara.app", message.allRecipients.first().toString())
    }
}
