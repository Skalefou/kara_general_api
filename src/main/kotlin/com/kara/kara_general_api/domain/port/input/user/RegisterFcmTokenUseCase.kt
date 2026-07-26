package com.kara.kara_general_api.domain.port.input.user

import com.kara.kara_general_api.domain.model.user.UserId

/**
 * Enregistre (ou remplace) le token d'appareil FCM d'un utilisateur pour lui adresser des notifications
 * push.
 */
interface RegisterFcmTokenUseCase {
    fun registerFcmToken(userId: UserId, token: String)
}
