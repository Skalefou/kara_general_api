package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PoolShareHoldReleaserTest {
    private val paymentGateway = mockk<PaymentGateway>(relaxed = true)
    private val sut = PoolShareHoldReleaser(paymentGateway)

    private val poolId = PoolId(UUID.randomUUID())
    private val payerId = UserId(UUID.randomUUID())

    private fun share(
        status: PoolShareStatus = PoolShareStatus.PENDING,
        intentId: String? = "pi_1",
    ) = PoolShare(
        PoolShareId(UUID.randomUUID()),
        poolId,
        "Créateur",
        null,
        BigDecimal("100.00"),
        status,
        intentId,
        null,
        payerId,
        true,
    )

    @Test
    fun `cancels and detaches the hold of a pending share`() {
        val released = sut.release(share())

        verify(exactly = 1) { paymentGateway.cancelPaymentIntent("pi_1") }
        assertNull(released.stripePaymentIntentId)
    }

    @Test
    fun `keeps the payer so the one-share-per-person rule still holds`() {
        // Le payeur reste attaché : c'est lui qui identifie « cet utilisateur détient déjà une part »
        // (SelfJoinPoolShareService). Le détacher laisserait le même utilisateur créer une seconde part.
        val released = sut.release(share())

        assertEquals(payerId, released.payerUserId)
    }

    @Test
    fun `does nothing for a share without any hold`() {
        val untouched = share(intentId = null)

        assertEquals(untouched, sut.release(untouched))
        verify(exactly = 0) { paymentGateway.cancelPaymentIntent(any()) }
    }

    @Test
    fun `leaves an already authorized or captured share untouched`() {
        // Ces parts sont refusées en amont par les gardes de statut des appelants : leur autorisation, elle,
        // est valide et ne doit jamais être libérée ici.
        listOf(PoolShareStatus.AUTHORIZED, PoolShareStatus.CAPTURED, PoolShareStatus.CANCELLED, PoolShareStatus.REFUNDED)
            .forEach { status ->
                val untouched = share(status = status)
                assertEquals(untouched, sut.release(untouched))
            }
        verify(exactly = 0) { paymentGateway.cancelPaymentIntent(any()) }
    }

    @Test
    fun `detaches the hold even when the gateway call fails`() {
        // Best-effort côté passerelle, jamais côté base : un intent qui reste attaché serait capturable à un
        // montant supérieur au dû. Au pire le blocage Stripe expire de lui-même.
        every { paymentGateway.cancelPaymentIntent("pi_1") } throws IllegalStateException("gateway down")

        val released = sut.release(share())

        assertNull(released.stripePaymentIntentId)
    }
}
