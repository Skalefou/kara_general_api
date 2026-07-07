package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.vo.Email
import java.time.Instant

interface EmailService {
    fun sendVerificationCode(email: Email, code: String)

    fun sendAccountDeletionConfirmation(email: Email)

    fun sendPasswordResetCode(email: Email, code: String)

    fun sendServerInvitation(email: Email, firstName: String, temporaryPassword: String, expiresAt: Instant)
}
