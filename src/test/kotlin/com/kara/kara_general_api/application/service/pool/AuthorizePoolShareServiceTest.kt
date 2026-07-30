package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.User
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.model.user.vo.HashedPassword
import com.kara.kara_general_api.domain.model.user.vo.PhoneNumber
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareResult
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PaymentIntentResult
import com.kara.kara_general_api.domain.port.output.PaymentIntentSnapshot
import com.kara.kara_general_api.domain.port.output.PaymentIntentStatus
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthorizePoolShareServiceTest {
    private val poolRepository = mockk<PoolRepository>()
    private val poolShareRepository = mockk<PoolShareRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val paymentGateway = mockk<PaymentGateway>()
    private val poolShareHoldReleaser = PoolShareHoldReleaser(paymentGateway)
    private val sut =
        AuthorizePoolShareService(poolRepository, poolShareRepository, userRepository, paymentGateway, poolShareHoldReleaser)

    private val poolId = PoolId(UUID.randomUUID())
    private val shareId = PoolShareId(UUID.randomUUID())
    private val payerId = UserId(UUID.randomUUID())

    private fun pool(
        status: PoolStatus = PoolStatus.OPEN,
        deadline: Instant = Instant.now().plusSeconds(3600),
    ) = Pool(poolId, BookingId(UUID.randomUUID()), BigDecimal("100.00"), Currency.EUR, status, deadline, "g", Instant.now())

    private fun share(status: PoolShareStatus = PoolShareStatus.PENDING) =
        PoolShare(shareId, poolId, "Alice", Email("a@b.com"), BigDecimal("50.00"), status, null, "tok", null, false)

    private fun payer() =
        User(
            id = payerId,
            email = Email("a@b.com"),
            hashedPassword = HashedPassword("h"),
            firstName = "Al",
            lastName = "Ice",
            phoneNumber = PhoneNumber("+33612345678"),
            birthDate = LocalDate.of(1990, 1, 1),
            role = UserRole.CLIENT,
            firebaseUid = "uid",
            createdAt = Instant.now(),
            stripeCustomerId = "cus_1",
        )

    private fun command() = AuthorizePoolShareCommand(poolId, shareId, payerId)

    @Test
    fun `returns PoolNotFound when the pool is missing`() {
        every { poolRepository.findByIdForUpdate(poolId) } returns null
        assertEquals(AuthorizePoolShareResult.PoolNotFound, sut.authorize(command()))
    }

    @Test
    fun `returns PoolClosed when the pool is not open`() {
        every { poolRepository.findByIdForUpdate(poolId) } returns pool(status = PoolStatus.SETTLED)
        assertEquals(AuthorizePoolShareResult.PoolClosed, sut.authorize(command()))
    }

    @Test
    fun `returns PoolExpired when the deadline has passed`() {
        every { poolRepository.findByIdForUpdate(poolId) } returns pool(deadline = Instant.now().minusSeconds(1))
        assertEquals(AuthorizePoolShareResult.PoolExpired, sut.authorize(command()))
    }

    @Test
    fun `returns ShareNotFound when the share does not belong to the pool`() {
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findById(shareId) } returns null
        assertEquals(AuthorizePoolShareResult.ShareNotFound, sut.authorize(command()))
    }

    @Test
    fun `returns ShareAlreadyProcessed when the share is not pending`() {
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findById(shareId) } returns share(status = PoolShareStatus.AUTHORIZED)
        assertEquals(AuthorizePoolShareResult.ShareAlreadyProcessed, sut.authorize(command()))
    }

    @Test
    fun `creates a manual-capture intent and links it to the share`() {
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findById(shareId) } returns share()
        every { userRepository.findById(payerId) } returns payer()
        every { paymentGateway.ensureCustomer(any()) } returns "cus_1"
        every { paymentGateway.createEphemeralKey("cus_1") } returns "ek"
        every { paymentGateway.createManualCapturePaymentIntent(BigDecimal("50.00"), Currency.EUR, "cus_1") } returns
            PaymentIntentResult(clientSecret = "cs", paymentIntentId = "pi_1")
        every { paymentGateway.publishableKey() } returns "pk"

        val result = sut.authorize(command())

        val ready = assertIs<AuthorizePoolShareResult.Ready>(result)
        assertEquals("cs", ready.clientSecret)
        assertEquals(shareId.value, ready.shareId)
        verify {
            poolShareRepository.save(
                match { it.stripePaymentIntentId == "pi_1" && it.payerUserId == payerId && it.status == PoolShareStatus.PENDING },
            )
        }
        // A manual-capture intent must be used (never the auto-capture pay-all variant).
        verify(exactly = 0) { paymentGateway.createPaymentIntent(any(), any(), any()) }
    }

    @Test
    fun `lets the creator authorize their own remainder share, with no special casing`() {
        // Une part de reliquat n'a pas de token de lien unique et est la seule à porter isCreatorShare : elle
        // doit malgré tout suivre exactement le même chemin de paiement que la part d'un participant.
        val remainder = share().copy(participantName = "Créateur", uniqueLinkToken = null, isCreatorShare = true)
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findById(shareId) } returns remainder
        every { userRepository.findById(payerId) } returns payer()
        every { paymentGateway.ensureCustomer(any()) } returns "cus_1"
        every { paymentGateway.createEphemeralKey("cus_1") } returns "ek"
        every { paymentGateway.createManualCapturePaymentIntent(BigDecimal("50.00"), Currency.EUR, "cus_1") } returns
            PaymentIntentResult(clientSecret = "cs", paymentIntentId = "pi_creator")
        every { paymentGateway.publishableKey() } returns "pk"

        val ready = assertIs<AuthorizePoolShareResult.Ready>(sut.authorize(command()))

        assertEquals("cs", ready.clientSecret)
        assertEquals(shareId.value, ready.shareId)
        // Autorisation créée pour le montant COURANT du reliquat, et rattachée au créateur comme payeur.
        verify(exactly = 1) { paymentGateway.createManualCapturePaymentIntent(BigDecimal("50.00"), Currency.EUR, "cus_1") }
        verify {
            poolShareRepository.save(
                match { it.isCreatorShare && it.stripePaymentIntentId == "pi_creator" && it.payerUserId == payerId },
            )
        }
    }

    @Test
    fun `re-serves the same client secret when the existing intent is still payable`() {
        // Given : le PaymentSheet a échoué, la part porte déjà un intent encore payable.
        val staleShare = share().withAuthorizationIntent("pi_stale", payerId)
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findById(shareId) } returns staleShare
        every { paymentGateway.retrievePaymentIntent("pi_stale") } returns
            PaymentIntentSnapshot("pi_stale", PaymentIntentStatus.REQUIRES_PAYMENT_METHOD, "cs_stale")
        every { userRepository.findById(payerId) } returns payer()
        every { paymentGateway.ensureCustomer(any()) } returns "cus_1"
        every { paymentGateway.createEphemeralKey("cus_1") } returns "ek"
        every { paymentGateway.publishableKey() } returns "pk"

        // When
        val ready = assertIs<AuthorizePoolShareResult.Ready>(sut.authorize(command()))

        // Then : le même intent est repris, sans second intent ni écriture (qui l'orphelinerait).
        assertEquals("cs_stale", ready.clientSecret)
        verify(exactly = 0) { paymentGateway.createManualCapturePaymentIntent(any(), any(), any()) }
        verify(exactly = 0) { poolShareRepository.save(any()) }
        verify(exactly = 0) { paymentGateway.cancelPaymentIntent(any()) }
    }

    @Test
    fun `returns ShareAlreadyProcessed when the existing intent already holds the funds`() {
        // Given : les fonds sont bloqués chez la passerelle, la part n'est plus à payer (le front réconcilie).
        val heldShare = share().withAuthorizationIntent("pi_held", payerId)
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findById(shareId) } returns heldShare
        every { paymentGateway.retrievePaymentIntent("pi_held") } returns
            PaymentIntentSnapshot("pi_held", PaymentIntentStatus.REQUIRES_CAPTURE, "cs_held")

        // When / Then
        assertEquals(AuthorizePoolShareResult.ShareAlreadyProcessed, sut.authorize(command()))
        verify(exactly = 0) { paymentGateway.createManualCapturePaymentIntent(any(), any(), any()) }
        verify(exactly = 0) { poolShareRepository.save(any()) }
    }

    @Test
    fun `releases the stale hold before creating a new intent`() {
        // Given : l'intent en cours est inexploitable (abandonné au 3-D Secure).
        val staleShare = share().withAuthorizationIntent("pi_stale", payerId)
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findById(shareId) } returns staleShare
        every { paymentGateway.retrievePaymentIntent("pi_stale") } returns
            PaymentIntentSnapshot("pi_stale", PaymentIntentStatus.REQUIRES_ACTION, "cs_stale")
        every { paymentGateway.cancelPaymentIntent("pi_stale") } returns Unit
        every { userRepository.findById(payerId) } returns payer()
        every { paymentGateway.ensureCustomer(any()) } returns "cus_1"
        every { paymentGateway.createEphemeralKey("cus_1") } returns "ek"
        every { paymentGateway.createManualCapturePaymentIntent(BigDecimal("50.00"), Currency.EUR, "cus_1") } returns
            PaymentIntentResult(clientSecret = "cs_new", paymentIntentId = "pi_new")
        every { paymentGateway.publishableKey() } returns "pk"

        // When
        val ready = assertIs<AuthorizePoolShareResult.Ready>(sut.authorize(command()))

        // Then : l'ancien blocage est libéré (jamais deux blocages sur la même carte) et le neuf est attaché.
        assertEquals("cs_new", ready.clientSecret)
        verify(exactly = 1) { paymentGateway.cancelPaymentIntent("pi_stale") }
        verify(exactly = 1) { poolShareRepository.save(match { it.stripePaymentIntentId == "pi_new" }) }
    }

    @Test
    fun `releases the stale hold and creates a new intent when the gateway cannot be reached`() {
        // Given : la relecture échoue (retrievePaymentIntent retourne null sans lever). On ne sait pas si
        // l'ancien intent bloque des fonds : il est libéré par précaution avant d'en créer un neuf.
        val staleShare = share().withAuthorizationIntent("pi_unknown", payerId)
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findById(shareId) } returns staleShare
        every { paymentGateway.retrievePaymentIntent("pi_unknown") } returns null
        every { paymentGateway.cancelPaymentIntent("pi_unknown") } returns Unit
        every { userRepository.findById(payerId) } returns payer()
        every { paymentGateway.ensureCustomer(any()) } returns "cus_1"
        every { paymentGateway.createEphemeralKey("cus_1") } returns "ek"
        every { paymentGateway.createManualCapturePaymentIntent(BigDecimal("50.00"), Currency.EUR, "cus_1") } returns
            PaymentIntentResult(clientSecret = "cs_new", paymentIntentId = "pi_new")
        every { paymentGateway.publishableKey() } returns "pk"

        // When
        val ready = assertIs<AuthorizePoolShareResult.Ready>(sut.authorize(command()))

        // Then
        assertEquals("cs_new", ready.clientSecret)
        verify(exactly = 1) { paymentGateway.cancelPaymentIntent("pi_unknown") }
        verify(exactly = 1) { poolShareRepository.save(match { it.stripePaymentIntentId == "pi_new" }) }
    }

    @Test
    fun `locks the pool before reading the share`() {
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        every { poolShareRepository.findById(shareId) } returns share(status = PoolShareStatus.AUTHORIZED)

        sut.authorize(command())

        // Sans ce verrou, l'upsert de la part (qui réécrit toute la ligne, montant compris) écraserait une
        // découpe du reliquat commitée entre la lecture et l'écriture.
        verify(exactly = 1) { poolRepository.findByIdForUpdate(poolId) }
        verify(exactly = 0) { poolRepository.findById(any()) }
    }
}
