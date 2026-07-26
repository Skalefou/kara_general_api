package com.kara.kara_general_api.application.service.user

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.user.RegisterFcmTokenUseCase
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service

/** Enregistre le token d'appareil FCM de l'utilisateur en le déléguant au [UserRepository]. */
@Service
class RegisterFcmTokenService(
    private val userRepository: UserRepository,
) : RegisterFcmTokenUseCase {

    override fun registerFcmToken(userId: UserId, token: String) {
        userRepository.updateFcmToken(userId, token)
    }
}
