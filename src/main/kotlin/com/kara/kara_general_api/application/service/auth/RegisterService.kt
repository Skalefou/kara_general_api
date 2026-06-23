package com.kara.kara_general_api.application.service.auth

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.port.input.auth.RegisterCommand
import com.kara.kara_general_api.domain.port.input.auth.RegisterResult
import com.kara.kara_general_api.domain.port.input.auth.RegisterUseCase
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service

@Service
class RegisterService(
    private val userRepository: UserRepository,
    private val firebaseAuthGateway: FirebaseAuthGateway,
    private val passwordHasher: PasswordHasher,
) : RegisterUseCase {

    override fun register(command: RegisterCommand): RegisterResult {
        if (userRepository.existsByEmail(command.email)) {
            return RegisterResult.EmailAlreadyUsed
        }

        val passwordIssues = validatePassword(command.plainPassword)
        if (passwordIssues.isNotEmpty()) {
            return RegisterResult.InvalidPassword(passwordIssues)
        }

        val firebaseUserId = firebaseAuthGateway.createUser(command.email, command.plainPassword)

        val user =
            try {
                val user =
                    User.register(
                        email = command.email,
                        hashedPassword = passwordHasher.hash(command.plainPassword),
                        firstName = command.firstName,
                        lastName = command.lastName,
                        phoneNumber = command.phoneNumber,
                        birthDate = command.birthDate,
                        firebaseUid = firebaseUserId.value,
                    )
                userRepository.save(user)
            } catch (e: Exception) {
                firebaseAuthGateway.deleteUser(firebaseUserId)
                throw e
            }

        return RegisterResult.Success(user)
    }

    private fun validatePassword(password: String): List<String> {
        val issues = mutableListOf<String>()
        if (password.length < 8) issues += "Le mot de passe doit contenir au moins 8 caractères"
        if (!password.any { it.isDigit() }) issues += "Le mot de passe doit contenir au moins un chiffre"
        if (!password.any { it.isLetter() }) issues += "Le mot de passe doit contenir au moins une lettre"
        return issues
    }
}