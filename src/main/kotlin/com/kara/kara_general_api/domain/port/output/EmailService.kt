package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.vo.Email

interface EmailService {
    fun sendVerificationCode(email: Email, code: String)

    fun sendAccountDeletionConfirmation(email: Email)
}
