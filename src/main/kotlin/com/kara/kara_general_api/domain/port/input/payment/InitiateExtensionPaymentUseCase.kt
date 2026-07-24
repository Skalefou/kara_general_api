package com.kara.kara_general_api.domain.port.input.payment

import com.kara.kara_general_api.domain.model.booking.BookingExtensionId
import com.kara.kara_general_api.domain.model.user.UserId
import java.util.UUID

data class InitiateExtensionPaymentCommand(
    val extensionId: BookingExtensionId,
    val userId: UserId,
)

sealed interface InitiateExtensionPaymentResult {
    data class Ready(
        val clientSecret: String,
        val ephemeralKeySecret: String,
        val customerId: String,
        val publishableKey: String,
        val paymentId: UUID,
    ) : InitiateExtensionPaymentResult

    data object ExtensionNotFound : InitiateExtensionPaymentResult

    data object NotOwner : InitiateExtensionPaymentResult

    data object AlreadySettled : InitiateExtensionPaymentResult

    data object ExtensionExpired : InitiateExtensionPaymentResult

    data object NotPayAll : InitiateExtensionPaymentResult
}

interface InitiateExtensionPaymentUseCase {
    fun initiate(command: InitiateExtensionPaymentCommand): InitiateExtensionPaymentResult
}
