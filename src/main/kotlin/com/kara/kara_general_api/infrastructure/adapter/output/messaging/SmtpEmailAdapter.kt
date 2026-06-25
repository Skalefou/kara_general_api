package com.kara.kara_general_api.infrastructure.adapter.output.messaging

import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.output.EmailService
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class SmtpEmailAdapter(
    private val mailSender: JavaMailSender,
    @Value("\${MAIL_FROM}") private val fromEmail: String,
) : EmailService {

    override fun sendVerificationCode(email: Email, code: String) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, false, "UTF-8")
        helper.setFrom(fromEmail)
        helper.setTo(email.value)
        helper.setSubject("Votre code de vérification Kara")
        helper.setText("<p>Votre code de vérification est : <strong>$code</strong></p>", true)
        mailSender.send(message)
    }
}
