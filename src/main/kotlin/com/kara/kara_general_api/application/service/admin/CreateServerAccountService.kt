package com.kara.kara_general_api.application.service.admin

import com.kara.kara_general_api.domain.model.user.PasswordPolicy
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.port.input.admin.CreateServerAccountCommand
import com.kara.kara_general_api.domain.port.input.admin.CreateServerAccountResult
import com.kara.kara_general_api.domain.port.input.admin.CreateServerAccountUseCase
import com.kara.kara_general_api.domain.port.output.EmailAlreadyUsedException
import com.kara.kara_general_api.domain.port.output.EmailService
import com.kara.kara_general_api.domain.port.output.FirebaseAuthGateway
import com.kara.kara_general_api.domain.port.output.PasswordGenerator
import com.kara.kara_general_api.domain.port.output.PasswordHasher
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

private val TEMP_PASSWORD_TTL: Duration = Duration.ofHours(24)

@Service
class CreateServerAccountService(
    private val userRepository: UserRepository,
    private val firebaseAuthGateway: FirebaseAuthGateway,
    private val passwordHasher: PasswordHasher,
    private val passwordGenerator: PasswordGenerator,
    private val emailService: EmailService,
) : CreateServerAccountUseCase {
    override fun createServerAccount(command: CreateServerAccountCommand): CreateServerAccountResult {
        if (userRepository.existsByEmail(command.email)) {
            return CreateServerAccountResult.EmailAlreadyUsed
        }

        val temporaryPassword = passwordGenerator.generate(UserRole.SERVER)
        check(PasswordPolicy.validate(temporaryPassword, UserRole.SERVER).isEmpty()) {
            "Generated temporary password does not satisfy the staff password policy"
        }
        val expiresAt = Instant.now().plus(TEMP_PASSWORD_TTL)

        val firebaseUserId =
            try {
                firebaseAuthGateway.createUser(command.email, temporaryPassword)
            } catch (_: EmailAlreadyUsedException) {
                return CreateServerAccountResult.EmailAlreadyUsed
            }

        val user =
            try {
                val user =
                    User.createServerAccount(
                        email = command.email,
                        hashedPassword = passwordHasher.hash(temporaryPassword),
                        firstName = command.firstName,
                        lastName = command.lastName,
                        phoneNumber = command.phoneNumber,
                        birthDate = command.birthDate,
                        firebaseUid = firebaseUserId.value,
                        tempPasswordExpiresAt = expiresAt,
                    )
                userRepository.save(user)
            } catch (e: Exception) {
                firebaseAuthGateway.deleteUser(firebaseUserId)
                throw e
            }

        emailService.sendServerInvitation(user.email, user.firstName, temporaryPassword, expiresAt)

        return CreateServerAccountResult.Success(user)
    }
}
