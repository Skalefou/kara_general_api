package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking.dto

import com.kara.kara_general_api.domain.model.booking.BookingExtension
import com.kara.kara_general_api.domain.port.input.booking.ExtensionOptions
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant

data class ExtensionQuoteResponse(
    val additionalMinutes: Int,
    val price: BigDecimal,
    val newEndAt: Instant,
)

data class ExtensionOptionsResponse(
    val bookingId: String,
    val currentEndAt: Instant,
    val maxAdditionalMinutes: Int,
    val extendable: Boolean,
    val currency: String,
    val settlementDeadline: Instant,
    val quotes: List<ExtensionQuoteResponse>,
) {
    companion object {
        fun from(options: ExtensionOptions): ExtensionOptionsResponse =
            ExtensionOptionsResponse(
                bookingId = options.bookingId.value.toString(),
                currentEndAt = options.currentEndAt,
                maxAdditionalMinutes = options.maxAdditionalMinutes,
                extendable = options.quotes.isNotEmpty(),
                currency = options.currency.name,
                settlementDeadline = options.settlementDeadline,
                quotes =
                    options.quotes.map {
                        ExtensionQuoteResponse(
                            additionalMinutes = it.additionalMinutes,
                            price = it.price,
                            newEndAt = it.newEndAt,
                        )
                    },
            )
    }
}

data class CreateExtensionRequest(
    @field:NotNull
    @field:Min(30)
    val additionalMinutes: Int?,
    @field:NotNull
    val paymentMode: String?,
)

data class BookingExtensionResponse(
    val id: String,
    val bookingId: String,
    val additionalMinutes: Int,
    val previousEndAt: Instant,
    val newEndAt: Instant,
    val price: BigDecimal,
    val currency: String,
    val status: String,
    val paymentMode: String,
    val expiresAt: Instant,
) {
    companion object {
        fun from(extension: BookingExtension): BookingExtensionResponse =
            BookingExtensionResponse(
                id = extension.id.value.toString(),
                bookingId = extension.bookingId.value.toString(),
                additionalMinutes = extension.additionalMinutes,
                previousEndAt = extension.previousEndAt,
                newEndAt = extension.newEndAt,
                price = extension.price,
                currency = extension.currency.name,
                status = extension.status.name,
                paymentMode = extension.paymentMode.name,
                expiresAt = extension.expiresAt,
            )
    }
}
