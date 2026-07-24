package com.kara.kara_general_api.infrastructure.adapter.input.rest.user

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.user.RegisterFcmTokenUseCase
import com.kara.kara_general_api.infrastructure.adapter.input.rest.user.dto.RegisterFcmTokenRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class FcmTokenController(
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase,
) : FcmTokenApi {

    override fun registerFcmToken(
        request: RegisterFcmTokenRequest,
        authentication: Authentication,
    ): ResponseEntity<Any> {
        registerFcmTokenUseCase.registerFcmToken(UserId(UUID.fromString(authentication.name)), request.token)
        return ResponseEntity.noContent().build()
    }
}
