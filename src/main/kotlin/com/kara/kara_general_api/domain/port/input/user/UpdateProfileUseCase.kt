package com.kara.kara_general_api.domain.port.input.user

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import java.time.LocalDate

data class UpdateProfileCommand(
    val userId: UserId,
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: PhoneNumber?,
    val birthDate: LocalDate?,
    val email: Email?,
)

sealed interface UpdateProfileResult {
    data class Success(val user: User) : UpdateProfileResult

    data object UserNotFound : UpdateProfileResult

    data object EmailAlreadyUsed : UpdateProfileResult
}

interface UpdateProfileUseCase {
    fun updateProfile(command: UpdateProfileCommand): UpdateProfileResult
}
