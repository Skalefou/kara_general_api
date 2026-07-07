package com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto

import com.kara.kara_general_api.domain.port.input.admin.AccountPage
import io.swagger.v3.oas.annotations.media.Schema

data class UserListResponse(
    @field:Schema(description = "Comptes de la page courante")
    val users: List<UserResponse>,
    @field:Schema(description = "Numéro de la page courante (0-indexed)")
    val page: Int,
    @field:Schema(description = "Nombre d'éléments par page")
    val size: Int,
    @field:Schema(description = "Nombre total de comptes")
    val totalElements: Long,
    @field:Schema(description = "Nombre total de pages")
    val totalPages: Int,
) {
    companion object {
        fun from(accountPage: AccountPage): UserListResponse =
            UserListResponse(
                users = accountPage.accounts.map(UserResponse::from),
                page = accountPage.page,
                size = accountPage.size,
                totalElements = accountPage.totalElements,
                totalPages =
                    if (accountPage.size == 0) {
                        0
                    } else {
                        ((accountPage.totalElements + accountPage.size - 1) / accountPage.size).toInt()
                    },
            )
    }
}
