package com.kara.kara_general_api.application.service.pool

import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.payment.Pool
import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import com.kara.kara_general_api.domain.port.input.pool.GetPoolRecapResult
import com.kara.kara_general_api.domain.port.input.pool.PoolRecapView
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
import kotlin.test.assertNull

class GetPoolRecapServiceTest {
    private val poolRepository = mockk<PoolRepository>()
    private val poolShareRepository = mockk<PoolShareRepository>()
    private val poolRecapAssembler = mockk<PoolRecapAssembler>()
    private val sut = GetPoolRecapService(poolRepository, poolShareRepository, poolRecapAssembler)

    private val poolId = PoolId(UUID.randomUUID())
    private val callerId = UserId(UUID.randomUUID())
    private val otherUserId = UserId(UUID.randomUUID())
    private val globalToken = "global-token"

    private fun pool() =
        Pool(
            poolId,
            BookingId(UUID.randomUUID()),
            BigDecimal("100.00"),
            Currency.EUR,
            PoolStatus.OPEN,
            Instant.now().plusSeconds(3600),
            globalToken,
            Instant.now(),
        )

    private fun callerShare() =
        PoolShare(
            PoolShareId(UUID.randomUUID()),
            poolId,
            "Alice",
            Email("a@b.com"),
            BigDecimal("40.00"),
            PoolShareStatus.PENDING,
            "pi_1",
            null,
            callerId,
            false,
        )

    private fun view(share: PoolShare?) =
        PoolRecapView(
            poolId = poolId.value,
            status = PoolStatus.OPEN,
            roomName = "Salle Étoile",
            startAt = Instant.parse("2026-08-01T18:00:00Z"),
            endAt = Instant.parse("2026-08-01T21:00:00Z"),
            numberOfPeople = 4,
            targetAmount = BigDecimal("100.00"),
            collectedAmount = BigDecimal("0.00"),
            currency = Currency.EUR,
            percentage = 0,
            deadline = Instant.parse("2026-08-01T12:00:00Z"),
            shareId = share?.id?.value,
            shareParticipantName = share?.participantName,
            shareAmount = share?.amount,
            shareStatus = share?.status,
        )

    @Test
    fun `returns the guest recap without touching the shares when there is no caller`() {
        // Given : lecture publique du lien global, sans authentification.
        val pool = pool()
        every { poolRepository.findByGlobalLinkToken(globalToken) } returns pool
        every { poolRecapAssembler.assemble(pool, null) } returns view(null)

        // When
        val result = sut.getByGlobalToken(globalToken, callerId = null)

        // Then : aucune part n'est jointe, et aucune requête de part n'est même émise.
        val found = result as GetPoolRecapResult.Found
        assertNull(found.view.shareId)
        verify(exactly = 1) { poolRecapAssembler.assemble(pool, null) }
        verify(exactly = 0) { poolShareRepository.findByPoolIdAndPayerUserId(any(), any()) }
    }

    @Test
    fun `joins the caller's own share so an interrupted payment can be resumed`() {
        // Given : l'appelant authentifié détient déjà une part de cette cagnotte.
        val pool = pool()
        val share = callerShare()
        every { poolRepository.findByGlobalLinkToken(globalToken) } returns pool
        every { poolShareRepository.findByPoolIdAndPayerUserId(poolId, callerId) } returns share
        every { poolRecapAssembler.assemble(pool, share) } returns view(share)

        // When
        val result = sut.getByGlobalToken(globalToken, callerId)

        // Then : le shareId est restitué, de quoi relancer le paiement de cette part.
        val found = result as GetPoolRecapResult.Found
        assertEquals(share.id.value, found.view.shareId)
        assertEquals(BigDecimal("40.00"), found.view.shareAmount)
    }

    @Test
    fun `returns a recap without share when the authenticated caller holds none`() {
        // Given : appelant authentifié, mais sans part dans cette cagnotte.
        val pool = pool()
        every { poolRepository.findByGlobalLinkToken(globalToken) } returns pool
        every { poolShareRepository.findByPoolIdAndPayerUserId(poolId, callerId) } returns null
        every { poolRecapAssembler.assemble(pool, null) } returns view(null)

        // When
        val result = sut.getByGlobalToken(globalToken, callerId)

        // Then
        assertNull((result as GetPoolRecapResult.Found).view.shareId)
    }

    @Test
    fun `only ever looks up the share of the caller that was provided`() {
        // Given : le filtrage est fait sur le payeur ; jamais de lecture pour un autre utilisateur.
        val pool = pool()
        val share = callerShare()
        every { poolRepository.findByGlobalLinkToken(globalToken) } returns pool
        every { poolShareRepository.findByPoolIdAndPayerUserId(poolId, callerId) } returns share
        every { poolRecapAssembler.assemble(pool, share) } returns view(share)

        // When
        sut.getByGlobalToken(globalToken, callerId)

        // Then
        verify(exactly = 1) { poolShareRepository.findByPoolIdAndPayerUserId(poolId, callerId) }
        verify(exactly = 0) { poolShareRepository.findByPoolIdAndPayerUserId(poolId, otherUserId) }
        verify(exactly = 0) { poolShareRepository.findByPoolId(any()) }
    }

    @Test
    fun `returns NotFound for an unknown global token, whether or not a caller is given`() {
        // Given
        every { poolRepository.findByGlobalLinkToken("nope") } returns null

        // When / Then : même réponse pour un invité et pour un utilisateur authentifié.
        assertEquals(GetPoolRecapResult.NotFound, sut.getByGlobalToken("nope", callerId = null))
        assertEquals(GetPoolRecapResult.NotFound, sut.getByGlobalToken("nope", callerId))
        verify(exactly = 0) { poolShareRepository.findByPoolIdAndPayerUserId(any(), any()) }
    }
}
