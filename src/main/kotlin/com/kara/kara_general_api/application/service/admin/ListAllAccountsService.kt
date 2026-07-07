package com.kara.kara_general_api.application.service.admin

import com.kara.kara_general_api.domain.port.input.admin.AccountPage
import com.kara.kara_general_api.domain.port.input.admin.ListAllAccountsQuery
import com.kara.kara_general_api.domain.port.input.admin.ListAllAccountsUseCase
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service

@Service
class ListAllAccountsService(
    private val userRepository: UserRepository,
) : ListAllAccountsUseCase {

    override fun listAllAccounts(query: ListAllAccountsQuery): AccountPage {
        val accounts = userRepository.findAll(page = query.page, size = query.size)
        val totalElements = userRepository.count()
        return AccountPage(accounts = accounts, page = query.page, size = query.size, totalElements = totalElements)
    }
}
