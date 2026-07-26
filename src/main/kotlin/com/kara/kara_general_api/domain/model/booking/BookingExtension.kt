package com.kara.kara_general_api.domain.model.booking

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

data class BookingExtension(
    val id: BookingExtensionId,
    val bookingId: BookingId,
    val userId: UserId,
    val additionalMinutes: Int,
    val previousEndAt: Instant,
    val newEndAt: Instant,
    val price: BigDecimal,
    val currency: Currency,
    val status: BookingExtensionStatus,
    val paymentMode: PaymentMode,
    val createdAt: Instant,
    val expiresAt: Instant,
) {
    fun confirm(): BookingExtension = copy(status = BookingExtensionStatus.CONFIRMED)

    fun cancel(): BookingExtension = copy(status = BookingExtensionStatus.CANCELLED)

    fun isPending(): Boolean = status == BookingExtensionStatus.PENDING

    companion object {
        val PAYMENT_WINDOW: Duration = Duration.ofMinutes(10)

        val MIN_ADDITIONAL_MINUTES: Int = 30

        val SETTLEMENT_MARGIN: Duration = Duration.ofMinutes(5)

        fun create(
            bookingId: BookingId,
            userId: UserId,
            additionalMinutes: Int,
            previousEndAt: Instant,
            price: BigDecimal,
            currency: Currency,
            paymentMode: PaymentMode,
            now: Instant,
        ): BookingExtension {
            val window = if (paymentMode == PaymentMode.SHARED_POT) sharedPotWindow(previousEndAt, now) else PAYMENT_WINDOW
            return BookingExtension(
                id = BookingExtensionId.generate(),
                bookingId = bookingId,
                userId = userId,
                additionalMinutes = additionalMinutes,
                previousEndAt = previousEndAt,
                newEndAt = previousEndAt.plus(Duration.ofMinutes(additionalMinutes.toLong())),
                price = price,
                currency = currency,
                status = BookingExtensionStatus.PENDING,
                paymentMode = paymentMode,
                createdAt = now,
                expiresAt = minOf(now.plus(window), settlementDeadline(previousEndAt)),
            )
        }

        fun settlementDeadline(previousEndAt: Instant): Instant = previousEndAt.minus(SETTLEMENT_MARGIN)

        private fun sharedPotWindow(
            previousEndAt: Instant,
            now: Instant,
        ): Duration = Duration.between(now, settlementDeadline(previousEndAt)).coerceAtLeast(Duration.ZERO)
    }
}
