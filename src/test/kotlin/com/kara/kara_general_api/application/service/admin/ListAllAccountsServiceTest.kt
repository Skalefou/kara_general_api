package com.kara.kara_general_api.application.service.admin

import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.admin.ListAllAccountsQuery
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class ListAllAccountsServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val sut = ListAllAccountsService(userRepository)

    private val account =
        User(
            id = UserId(UUID.randomUUID()),
            email = Email("server@kara.app"),
            hashedPassword = HashedPassword("hashed"),
            firstName = "Jane",
            lastName = "Doe",
            phoneNumber = PhoneNumber("0612345678"),
            birthDate = LocalDate.of(1990, 1, 15),
            role = UserRole.SERVER,
            firebaseUid = "firebase-uid",
            createdAt = Instant.now(),
        )

    @Test
    fun `should return a page of accounts with the total count`() {
        every { userRepository.findAll(page = 1, size = 10) } returns listOf(account)
        every { userRepository.count() } returns 42

        val result = sut.listAllAccounts(ListAllAccountsQuery(page = 1, size = 10))

        assertEquals(listOf(account), result.accounts)
        assertEquals(1, result.page)
        assertEquals(10, result.size)
        assertEquals(42, result.totalElements)
    }
}
