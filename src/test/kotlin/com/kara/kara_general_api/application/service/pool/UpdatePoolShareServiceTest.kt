package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.pool.UpdatePoolShareCommand
import com.kara.kara_general_api.domain.port.input.pool.UpdatePoolShareResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.PaymentGateway
import com.kara.kara_general_api.domain.port.output.PoolRepository
import com.kara.kara_general_api.domain.port.output.PoolShareRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpdatePoolShareServiceTest {
    private val poolRepository = mockk<PoolRepository>()
    private val poolShareRepository = mockk<PoolShareRepository>(relaxed = true)
    private val bookingRepository = mockk<BookingRepository>()
    private val poolLinkBuilder = FakePoolLinkBuilder()
    private val paymentGateway = mockk<PaymentGateway>(relaxed = true)

    // Releaser réel (et non mock) : la libération de l'autorisation obsolète fait partie du comportement testé.
    private val poolShareHoldReleaser = PoolShareHoldReleaser(paymentGateway)
    private val sut =
        UpdatePoolShareService(poolRepository, poolShareRepository, bookingRepository, poolLinkBuilder, poolShareHoldReleaser)

    private val poolId = PoolId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())
    private val creatorId = UserId(UUID.randomUUID())
    private val targetId = PoolShareId(UUID.randomUUID())

    private fun pool() =
        Pool(poolId, bookingId, BigDecimal("100.00"), Currency.EUR, PoolStatus.OPEN, Instant.now().plusSeconds(3600), "g", Instant.now())

    private fun creatorShare(amount: String = "60.00") =
        PoolShare(
            PoolShareId(UUID.randomUUID()),
            poolId,
            "Créateur",
            null,
            BigDecimal(amount),
            PoolShareStatus.PENDING,
            null,
            null,
            creatorId,
            true,
        )

    private fun targetShare(
        amount: String = "40.00",
        status: PoolShareStatus = PoolShareStatus.PENDING,
    ) = PoolShare(targetId, poolId, "Bob", null, BigDecimal(amount), status, null, "tok", null, false)

    private fun stubOwnership() {
        every { poolRepository.findByIdForUpdate(poolId) } returns pool()
        val booking = mockk<Booking>()
        every { booking.userId } returns creatorId
        every { bookingRepository.findById(bookingId) } returns booking
    }

    private fun command(amount: String) = UpdatePoolShareCommand(poolId, targetId, creatorId, BigDecimal(amount))

    @Test
    fun `rejects editing a share already authorized`() {
        stubOwnership()
        every { poolShareRepository.findById(targetId) } returns targetShare(status = PoolShareStatus.AUTHORIZED)

        assertEquals(UpdatePoolShareResult.ShareAlreadyPaid, sut.updateShare(command("50.00")))
    }

    @Test
    fun `rejects editing the creator remainder directly`() {
        stubOwnership()
        every { poolShareRepository.findById(targetId) } returns
            PoolShare(targetId, poolId, "Créateur", null, BigDecimal("60.00"), PoolShareStatus.PENDING, null, null, creatorId, true)

        assertEquals(UpdatePoolShareResult.CannotEditCreatorShare, sut.updateShare(command("50.00")))
    }

    @Test
    fun `rebalances the creator remainder to preserve the invariant`() {
        stubOwnership()
        every { poolShareRepository.findById(targetId) } returns targetShare(amount = "40.00")
        every { poolShareRepository.findByPoolId(poolId) } returns listOf(creatorShare(amount = "60.00"), targetShare(amount = "40.00"))

        // Raise the target share from 40 to 50 → creator remainder must drop from 60 to 50.
        val result = sut.updateShare(command("50.00"))

        assertIs<UpdatePoolShareResult.Updated>(result)
        verify { poolShareRepository.save(match { it.isCreatorShare && it.amount == BigDecimal("50.00") }) }
        verify { poolShareRepository.save(match { it.id == targetId && it.amount == BigDecimal("50.00") }) }
    }

    @Test
    fun `rebalancing releases and detaches the Stripe holds of both re-priced shares`() {
        // Les deux montants changent : conserver leurs autorisations en cours ferait capturer un montant qui
        // n'est plus le dû (trop pour le reliquat qui baisse, trop peu pour la part qui monte).
        stubOwnership()
        val target = targetShare(amount = "40.00").withAuthorizationIntent("pi_target", creatorId)
        every { poolShareRepository.findById(targetId) } returns target
        every { poolShareRepository.findByPoolId(poolId) } returns
            listOf(creatorShare(amount = "60.00").withAuthorizationIntent("pi_creator", creatorId), target)

        assertIs<UpdatePoolShareResult.Updated>(sut.updateShare(command("50.00")))

        verify(exactly = 1) { paymentGateway.cancelPaymentIntent("pi_creator") }
        verify(exactly = 1) { paymentGateway.cancelPaymentIntent("pi_target") }
        verify { poolShareRepository.save(match { it.isCreatorShare && it.stripePaymentIntentId == null }) }
        verify { poolShareRepository.save(match { it.id == targetId && it.stripePaymentIntentId == null }) }
    }

    @Test
    fun `locks the pool before reading the shares`() {
        stubOwnership()
        every { poolShareRepository.findById(targetId) } returns targetShare(status = PoolShareStatus.AUTHORIZED)

        sut.updateShare(command("50.00"))

        verify(exactly = 1) { poolRepository.findByIdForUpdate(poolId) }
        verify(exactly = 0) { poolRepository.findById(any()) }
    }

    @Test
    fun `rejects when rebalancing would exhaust the creator remainder`() {
        stubOwnership()
        every { poolShareRepository.findById(targetId) } returns targetShare(amount = "40.00")
        every { poolShareRepository.findByPoolId(poolId) } returns listOf(creatorShare(amount = "60.00"), targetShare(amount = "40.00"))

        // Raising the target to 100 would drop the creator remainder to 0.
        assertEquals(UpdatePoolShareResult.InsufficientRemainder, sut.updateShare(command("100.00")))
    }
}
