package com.kara.kara_general_api.application.service.payment

import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.payment.Payment
import com.kara.kara_general_api.domain.port.input.payment.InitiateExtensionPaymentCommand
import com.kara.kara_general_api.domain.port.input.payment.InitiateExtensionPaymentResult
import com.kara.kara_general_api.domain.port.input.payment.InitiateExtensionPaymentUseCase
import com.kara.kara_general_api.domain.port.output.BookingExtensionRepository
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class InitiateExtensionPaymentService(
    private val bookingExtensionRepository: BookingExtensionRepository,
    private val userRepository: UserRepository,
    private val paymentGateway: PaymentGateway,
    private val paymentRepository: PaymentRepository,
) : InitiateExtensionPaymentUseCase {
    @Transactional
    override fun initiate(command: InitiateExtensionPaymentCommand): InitiateExtensionPaymentResult {
        val extension =
            bookingExtensionRepository.findById(command.extensionId)
                ?: return InitiateExtensionPaymentResult.ExtensionNotFound
        if (extension.userId != command.userId) return InitiateExtensionPaymentResult.NotOwner
        if (!extension.isPending()) return InitiateExtensionPaymentResult.AlreadySettled
        if (extension.paymentMode != PaymentMode.PAY_ALL) return InitiateExtensionPaymentResult.NotPayAll
        if (!extension.expiresAt.isAfter(Instant.now())) return InitiateExtensionPaymentResult.ExtensionExpired

        val user =
            userRepository.findById(command.userId) ?: return InitiateExtensionPaymentResult.ExtensionNotFound

        val customerId = paymentGateway.ensureCustomer(user)
        if (user.stripeCustomerId != customerId) {
            userRepository.updateStripeCustomerId(user.id, customerId)
        }

        val ephemeralKeySecret = paymentGateway.createEphemeralKey(customerId)
        val intent = paymentGateway.createPaymentIntent(extension.price, extension.currency, customerId)

        val payment =
            paymentRepository.save(
                Payment.pending(
                    bookingId = extension.bookingId,
                    userId = user.id,
                    amount = extension.price,
                    currency = extension.currency,
                    stripePaymentIntentId = intent.paymentIntentId,
                    extensionId = extension.id,
                ),
            )

        return InitiateExtensionPaymentResult.Ready(
            clientSecret = intent.clientSecret,
            ephemeralKeySecret = ephemeralKeySecret,
            customerId = customerId,
            publishableKey = paymentGateway.publishableKey(),
            paymentId = payment.id.value,
        )
    }
}
