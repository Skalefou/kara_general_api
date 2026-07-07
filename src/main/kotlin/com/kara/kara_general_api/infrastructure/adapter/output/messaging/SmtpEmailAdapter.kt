package com.kara.kara_general_api.infrastructure.adapter.output.messaging

import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.output.EmailService
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    override fun sendAccountDeletionConfirmation(email: Email) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, false, "UTF-8")
        helper.setFrom(fromEmail)
        helper.setTo(email.value)
        helper.setSubject("Votre compte Kara a été supprimé")
        helper.setText(
            "<p>Votre compte a bien été supprimé. Toutes vos données personnelles ont été effacées.</p>",
            true,
        )
        mailSender.send(message)
    }

    override fun sendPasswordResetCode(email: Email, code: String) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, false, "UTF-8")
        helper.setFrom(fromEmail)
        helper.setTo(email.value)
        helper.setSubject("Réinitialisation de votre mot de passe Kara")
        helper.setText(
            "<p>Votre code de réinitialisation est : <strong>$code</strong></p>" +
                "<p>Ce code est valable 15 minutes.</p>",
            true,
        )
        mailSender.send(message)
    }

    override fun sendServerInvitation(email: Email, firstName: String, temporaryPassword: String, expiresAt: Instant) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, false, "UTF-8")
        helper.setFrom(fromEmail)
        helper.setTo(email.value)
        helper.setSubject("Votre compte serveur Kara")
        helper.setText(
            "<p>Bonjour $firstName,</p>" +
                "<p>Un compte serveur Kara a été créé pour vous.</p>" +
                "<p>Mot de passe temporaire : <strong>$temporaryPassword</strong></p>" +
                "<p>Ce mot de passe est valable jusqu'au ${INVITATION_DATE_FORMATTER.format(expiresAt)}. " +
                "Vous devrez le changer lors de votre première connexion.</p>",
            true,
        )
        mailSender.send(message)
    }

    private companion object {
        val INVITATION_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm", Locale.FRANCE)
                .withZone(ZoneId.of("Europe/Paris"))
    }
}
