package com.kara.kara_general_api.domain.model.payment

import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PoolShareTest {

    private val poolId = PoolId(UUID.randomUUID())

    private fun share() =
        PoolShare.create(
            poolId = poolId,
            participantName = "Alice",
            email = Email("alice@example.com"),
            amount = BigDecimal("50.00"),
            uniqueLinkToken = "tok",
            isCreatorShare = false,
        )

    @Test
    fun `create rejects a non-positive amount`() {
        assertThrows<IllegalArgumentException> {
            PoolShare.create(poolId, "Bob", null, BigDecimal.ZERO, null, false)
        }
    }

    @Test
    fun `create rejects a blank participant name`() {
        assertThrows<IllegalArgumentException> {
            PoolShare.create(poolId, " ", null, BigDecimal("10.00"), null, false)
        }
    }

    @Test
    fun `create starts PENDING without intent or payer`() {
        val s = share()

        assertEquals(PoolShareStatus.PENDING, s.status)
        assertNull(s.stripePaymentIntentId)
        assertNull(s.payerUserId)
    }

    @Test
    fun `withAuthorizationIntent links the intent and payer but keeps the share PENDING`() {
        val payer = UserId(UUID.randomUUID())

        val s = share().withAuthorizationIntent("pi_1", payer)

        assertEquals("pi_1", s.stripePaymentIntentId)
        assertEquals(payer, s.payerUserId)
        assertEquals(PoolShareStatus.PENDING, s.status)
    }

    @Test
    fun `status transitions and settleability`() {
        val s = share()

        assertFalse(s.isSettleable())
        assertTrue(s.markAuthorized().isSettleable())
        assertTrue(s.markCaptured().isSettleable())
        assertFalse(s.markCancelled().isSettleable())
    }

    @Test
    fun `updateAmount rejects a non-positive amount`() {
        assertThrows<IllegalArgumentException> { share().updateAmount(BigDecimal("-1")) }
    }
}
