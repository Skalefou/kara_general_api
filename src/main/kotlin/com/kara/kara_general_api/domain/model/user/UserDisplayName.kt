package com.kara.kara_general_api.domain.model.user

fun User?.displayName(fallback: String = ""): String =
    this?.let { "${it.firstName} ${it.lastName}".trim() } ?: fallback
