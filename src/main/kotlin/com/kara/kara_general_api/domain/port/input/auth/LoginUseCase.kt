package com.kara.kara_general_api.domain.port.input.auth

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.output.AccessToken

sealed interface LoginIdentifier {
    data class ByEmail(val email: Email) : LoginIdentifier

    data class ByPhoneNumber(val phoneNumber: PhoneNumber) : LoginIdentifier
}

data class LoginCommand(
    val identifier: LoginIdentifier,
    val password: String,
)

sealed interface LoginResult {
    data class Success(val user: User, val accessToken: AccessToken) : LoginResult

    data object UserNotFound : LoginResult

    data object InvalidCredentials : LoginResult

    data object AccountDeleted : LoginResult
}

interface LoginUseCase {
    fun login(command: LoginCommand): LoginResult
}
