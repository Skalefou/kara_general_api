package com.kara.kara_general_api.domain.port.input.auth

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import java.time.LocalDate

interface RegisterUseCase {
    fun register(command: RegisterCommand): RegisterResult
}

data class RegisterCommand(
    val email: Email,
    val plainPassword: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: PhoneNumber,
    val birthDate: LocalDate,
)

sealed interface RegisterResult {
    data class Success(
        val user: User,
    ) : RegisterResult

    data object EmailAlreadyUsed : RegisterResult

    data class InvalidPassword(
        val reasons: List<String>,
    ) : RegisterResult
}
