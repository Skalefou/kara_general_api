package com.kara.kara_general_api.infrastructure.adapter.output.payment

import com.stripe.model.EphemeralKey
import com.stripe.param.EphemeralKeyCreateParams
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StripePaymentGatewayAdapterTest {

    private val mobileApiVersion = "2020-08-27"

    private val adapter =
        StripePaymentGatewayAdapter(
            publishableKey = "pk_test",
            webhookSecret = "whsec_test",
            mobileApiVersion = mobileApiVersion,
        )

    @AfterEach
    fun tearDown() {
        unmockkStatic(EphemeralKey::class)
    }

    @Test
    fun `should seal the ephemeral key on the mobile SDK API version, not the server library version`() {
        mockkStatic(EphemeralKey::class)
        val paramsSlot = slot<EphemeralKeyCreateParams>()
        val ephemeralKey = mockk<EphemeralKey>()
        every { ephemeralKey.secret } returns "ek_secret"
        every { EphemeralKey.create(capture(paramsSlot)) } returns ephemeralKey

        val secret = adapter.createEphemeralKey("cus_123")

        assertEquals("ek_secret", secret)
        // La version doit être posée sur les params (EphemeralKey.create valide params.getStripeVersion()),
        // et vaut la version du SDK mobile (flutter_stripe), pas Stripe.API_VERSION.
        assertEquals(mobileApiVersion, paramsSlot.captured.stripeVersion)
        verify(exactly = 1) { EphemeralKey.create(any<EphemeralKeyCreateParams>()) }
    }
}
