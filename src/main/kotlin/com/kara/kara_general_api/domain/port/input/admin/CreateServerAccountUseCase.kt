package com.kara.kara_general_api.domain.port.input.admin

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import java.time.LocalDate

data class CreateServerAccountCommand(
    val email: Email,
    val firstName: String,
    val lastName: String,
    val phoneNumber: PhoneNumber,
    val birthDate: LocalDate,
)

sealed interface CreateServerAccountResult {
    data class Success(val user: User) : CreateServerAccountResult

    data object EmailAlreadyUsed : CreateServerAccountResult
}

interface CreateServerAccountUseCase {
    fun createServerAccount(command: CreateServerAccountCommand): CreateServerAccountResult
}
