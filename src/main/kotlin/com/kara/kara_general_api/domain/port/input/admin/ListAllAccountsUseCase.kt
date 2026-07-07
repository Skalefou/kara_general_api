package com.kara.kara_general_api.domain.port.input.admin

import com.kara.kara_general_api.domain.model.user.User

data class ListAllAccountsQuery(
    val page: Int = 0,
    val size: Int = 20,
)

data class AccountPage(
    val accounts: List<User>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
)

interface ListAllAccountsUseCase {
    fun listAllAccounts(query: ListAllAccountsQuery): AccountPage
}
