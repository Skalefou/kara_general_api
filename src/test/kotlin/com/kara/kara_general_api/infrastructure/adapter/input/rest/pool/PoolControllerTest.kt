package com.kara.kara_general_api.infrastructure.adapter.input.rest.pool

import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.port.input.pool.AddPoolShareUseCase
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.AuthorizePoolShareUseCase
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolResult
import com.kara.kara_general_api.domain.port.input.pool.CreatePoolUseCase
import com.kara.kara_general_api.domain.port.input.pool.GetPoolRecapResult
import com.kara.kara_general_api.domain.port.input.pool.GetPoolRecapUseCase
import com.kara.kara_general_api.domain.port.input.pool.GetPoolResult
import com.kara.kara_general_api.domain.port.input.pool.GetPoolUseCase
import com.kara.kara_general_api.domain.port.input.pool.ListUserPoolsUseCase
import com.kara.kara_general_api.domain.port.input.pool.PoolRecapView
import com.kara.kara_general_api.domain.port.input.pool.PoolShareView
import com.kara.kara_general_api.domain.port.input.pool.PoolSummaryView
import com.kara.kara_general_api.domain.port.input.pool.PoolView
import com.kara.kara_general_api.domain.port.input.pool.RegeneratePoolLinkResult
import com.kara.kara_general_api.domain.port.input.pool.RegeneratePoolLinkUseCase
import com.kara.kara_general_api.domain.port.input.pool.RemindPoolShareUseCase
import com.kara.kara_general_api.domain.port.input.pool.SelfJoinPoolShareResult
import com.kara.kara_general_api.domain.port.input.pool.SelfJoinPoolShareUseCase
import com.kara.kara_general_api.domain.port.input.pool.UpdatePoolShareUseCase
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

private const val USER_ID = "11111111-2222-3333-4444-555555555555"
private const val BOOKING_ID = "550e8400-e29b-41d4-a716-446655440000"
private const val POOL_ID = "660e8400-e29b-41d4-a716-446655440000"
private const val SHARE_ID = "770e8400-e29b-41d4-a716-446655440000"
private const val CREATOR_SHARE_ID = "880e8400-e29b-41d4-a716-446655440000"

// Base des liens de partage : les vues portent déjà les URLs construites côté application,
// le contrôleur ne fait que les recopier dans la réponse.
private const val LINK_BASE_URL = "https://link.karapi.fr"

@WebMvcTest(PoolController::class)
@Import(SecurityConfig::class)
class PoolControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var createPoolUseCase: CreatePoolUseCase

    @MockkBean
    private lateinit var getPoolUseCase: GetPoolUseCase

    @MockkBean
    private lateinit var getPoolRecapUseCase: GetPoolRecapUseCase

    @MockkBean
    private lateinit var authorizePoolShareUseCase: AuthorizePoolShareUseCase

    @MockkBean
    private lateinit var addPoolShareUseCase: AddPoolShareUseCase

    @MockkBean
    private lateinit var updatePoolShareUseCase: UpdatePoolShareUseCase

    @MockkBean
    private lateinit var regeneratePoolLinkUseCase: RegeneratePoolLinkUseCase

    @MockkBean
    private lateinit var remindPoolShareUseCase: RemindPoolShareUseCase

    @MockkBean
    private lateinit var listUserPoolsUseCase: ListUserPoolsUseCase

    @MockkBean
    private lateinit var selfJoinPoolShareUseCase: SelfJoinPoolShareUseCase

    private fun poolView() =
        PoolView(
            poolId = UUID.fromString(POOL_ID),
            bookingId = UUID.fromString(BOOKING_ID),
            status = PoolStatus.OPEN,
            targetAmount = BigDecimal("100.00"),
            collectedAmount = BigDecimal("40.00"),
            currency = Currency.EUR,
            percentage = 40,
            deadline = Instant.parse("2026-08-01T12:00:00Z"),
            globalLinkToken = "global-token",
            globalShareUrl = "$LINK_BASE_URL/join/global-token",
            shares =
                listOf(
                    PoolShareView(
                        shareId = UUID.fromString(SHARE_ID),
                        participantName = "Bob",
                        email = "bob@example.com",
                        amount = BigDecimal("40.00"),
                        status = PoolShareStatus.PENDING,
                        isCreatorShare = false,
                        uniqueLinkToken = "share-token",
                        shareUrl = "$LINK_BASE_URL/p/share-token",
                    ),
                    PoolShareView(
                        shareId = UUID.fromString(CREATOR_SHARE_ID),
                        participantName = "Moi",
                        email = null,
                        amount = BigDecimal("60.00"),
                        status = PoolShareStatus.PENDING,
                        isCreatorShare = true,
                        uniqueLinkToken = null,
                        shareUrl = null,
                    ),
                ),
        )

    private val createBody =
        """{"bookingId":"$BOOKING_ID","shares":[{"participantName":"Moi","amount":100.00,"isCreatorShare":true}]}"""

    @Test
    fun `should return 401 when listing pools without authentication`() {
        mockMvc.perform(get("/api/v1/pools")).andExpect(status().isUnauthorized)

        verify(exactly = 0) { listUserPoolsUseCase.listForUser(any()) }
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with the user's pool summaries`() {
        every { listUserPoolsUseCase.listForUser(any()) } returns
            listOf(
                PoolSummaryView(
                    poolId = UUID.fromString(POOL_ID),
                    bookingId = UUID.fromString(BOOKING_ID),
                    roomName = "Salle Étoile",
                    startAt = Instant.parse("2026-08-01T18:00:00Z"),
                    status = PoolStatus.OPEN,
                    targetAmount = BigDecimal("100.00"),
                    collectedAmount = BigDecimal("40.00"),
                    currency = Currency.EUR,
                    percentage = 40,
                    deadline = Instant.parse("2026-08-01T12:00:00Z"),
                    isCreator = true,
                ),
            )

        mockMvc
            .perform(get("/api/v1/pools"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].poolId").value(POOL_ID))
            .andExpect(jsonPath("$[0].roomName").value("Salle Étoile"))
            .andExpect(jsonPath("$[0].isCreator").value(true))
            .andExpect(jsonPath("$[0].percentage").value(40))
    }

    @Test
    fun `should return 401 when creating a pool without authentication`() {
        mockMvc
            .perform(
                post("/api/v1/pools").contentType(MediaType.APPLICATION_JSON).content(createBody),
            ).andExpect(status().isUnauthorized)

        verify(exactly = 0) { createPoolUseCase.create(any()) }
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 201 with the pool when created`() {
        every { createPoolUseCase.create(any()) } returns CreatePoolResult.Created(poolView())

        mockMvc
            .perform(
                post("/api/v1/pools").contentType(MediaType.APPLICATION_JSON).content(createBody),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.poolId").value(POOL_ID))
            .andExpect(jsonPath("$.globalLinkToken").value("global-token"))
            .andExpect(jsonPath("$.percentage").value(40))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should expose the server-built global share url alongside the global token`() {
        every { getPoolUseCase.getById(any(), any()) } returns GetPoolResult.Found(poolView())

        mockMvc
            .perform(get("/api/v1/pools/$POOL_ID"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.globalLinkToken").value("global-token"))
            .andExpect(jsonPath("$.globalShareUrl").value("$LINK_BASE_URL/join/global-token"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should expose the server-built share url for a share that has a unique token`() {
        every { getPoolUseCase.getById(any(), any()) } returns GetPoolResult.Found(poolView())

        mockMvc
            .perform(get("/api/v1/pools/$POOL_ID"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.shares[0].uniqueLinkToken").value("share-token"))
            .andExpect(jsonPath("$.shares[0].shareUrl").value("$LINK_BASE_URL/p/share-token"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return a null share url for a share without a unique token`() {
        every { getPoolUseCase.getById(any(), any()) } returns GetPoolResult.Found(poolView())

        mockMvc
            .perform(get("/api/v1/pools/$POOL_ID"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.shares[1].shareUrl").doesNotExist())
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return the regenerated global token with its share url`() {
        every { regeneratePoolLinkUseCase.regenerate(any()) } returns
            RegeneratePoolLinkResult.Regenerated(
                globalLinkToken = "new-global-token",
                globalShareUrl = "$LINK_BASE_URL/join/new-global-token",
            )

        mockMvc
            .perform(post("/api/v1/pools/$POOL_ID/regenerate-link"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.globalLinkToken").value("new-global-token"))
            .andExpect(jsonPath("$.globalShareUrl").value("$LINK_BASE_URL/join/new-global-token"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 with POOL_SHARES_MISMATCH when shares do not sum to the target`() {
        every { createPoolUseCase.create(any()) } returns
            CreatePoolResult.SharesMismatch(expected = BigDecimal("100.00"), actual = BigDecimal("90.00"))

        mockMvc
            .perform(
                post("/api/v1/pools").contentType(MediaType.APPLICATION_JSON).content(createBody),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("POOL_SHARES_MISMATCH"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 403 when reading a pool the caller does not own`() {
        every { getPoolUseCase.getById(any(), any()) } returns GetPoolResult.NotOwner

        mockMvc
            .perform(get("/api/v1/pools/$POOL_ID"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("POOL_NOT_OWNER"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with the PaymentSheet secrets when authorizing a share`() {
        every { authorizePoolShareUseCase.authorize(any()) } returns
            AuthorizePoolShareResult.Ready(
                clientSecret = "cs",
                ephemeralKeySecret = "ek",
                customerId = "cus_1",
                publishableKey = "pk",
                shareId = UUID.fromString(SHARE_ID),
            )

        mockMvc
            .perform(post("/api/v1/pools/$POOL_ID/shares/$SHARE_ID/payment"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.clientSecret").value("cs"))
            .andExpect(jsonPath("$.shareId").value(SHARE_ID))
    }

    @Test
    fun `should expose the public join recap without authentication`() {
        every { getPoolRecapUseCase.getByGlobalToken("global-token") } returns
            GetPoolRecapResult.Found(
                PoolRecapView(
                    poolId = UUID.fromString(POOL_ID),
                    status = PoolStatus.OPEN,
                    roomName = "Salle Étoile",
                    startAt = Instant.parse("2026-08-01T18:00:00Z"),
                    endAt = Instant.parse("2026-08-01T21:00:00Z"),
                    numberOfPeople = 4,
                    targetAmount = BigDecimal("100.00"),
                    collectedAmount = BigDecimal("40.00"),
                    currency = Currency.EUR,
                    percentage = 40,
                    deadline = Instant.parse("2026-08-01T12:00:00Z"),
                    shareId = null,
                    shareParticipantName = null,
                    shareAmount = null,
                    shareStatus = null,
                ),
            )

        mockMvc
            .perform(get("/api/v1/pools/join/global-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.booking.roomName").value("Salle Étoile"))
            .andExpect(jsonPath("$.message").isNotEmpty)
    }

    @Test
    fun `should resolve a pool from a unique share token without authentication`() {
        every { getPoolRecapUseCase.getByShareToken("share-token") } returns
            GetPoolRecapResult.Found(
                PoolRecapView(
                    poolId = UUID.fromString(POOL_ID),
                    status = PoolStatus.OPEN,
                    roomName = "Salle Étoile",
                    startAt = Instant.parse("2026-08-01T18:00:00Z"),
                    endAt = Instant.parse("2026-08-01T21:00:00Z"),
                    numberOfPeople = 4,
                    targetAmount = BigDecimal("100.00"),
                    collectedAmount = BigDecimal("40.00"),
                    currency = Currency.EUR,
                    percentage = 40,
                    deadline = Instant.parse("2026-08-01T12:00:00Z"),
                    shareId = UUID.fromString(SHARE_ID),
                    shareParticipantName = "Bob",
                    shareAmount = BigDecimal("40.00"),
                    shareStatus = PoolShareStatus.PENDING,
                ),
            )

        mockMvc
            .perform(get("/api/v1/pools/share/share-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.poolId").value(POOL_ID))
            .andExpect(jsonPath("$.booking.roomName").value("Salle Étoile"))
            .andExpect(jsonPath("$.share.shareId").value(SHARE_ID))
            .andExpect(jsonPath("$.share.participantName").value("Bob"))
    }

    @Test
    fun `should return 404 for an unknown share recap`() {
        every { getPoolRecapUseCase.getByShareToken("nope") } returns GetPoolRecapResult.NotFound

        mockMvc
            .perform(get("/api/v1/pools/share/nope"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("POOL_NOT_FOUND"))
    }

    private val selfJoinBody = """{"amount":30.00}"""

    private fun performSelfJoin() =
        mockMvc.perform(
            post("/api/v1/pools/join/global-token/shares")
                .contentType(MediaType.APPLICATION_JSON)
                .content(selfJoinBody),
        )

    @Test
    fun `should return 401 when self-joining a pool without authentication`() {
        performSelfJoin().andExpect(status().isUnauthorized)

        verify(exactly = 0) { selfJoinPoolShareUseCase.selfJoin(any()) }
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with the PaymentSheet secrets when self-joining`() {
        every { selfJoinPoolShareUseCase.selfJoin(any()) } returns
            SelfJoinPoolShareResult.Ready(
                clientSecret = "cs",
                ephemeralKeySecret = "ek",
                customerId = "cus_1",
                publishableKey = "pk",
                shareId = UUID.fromString(SHARE_ID),
            )

        performSelfJoin()
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.clientSecret").value("cs"))
            .andExpect(jsonPath("$.ephemeralKeySecret").value("ek"))
            .andExpect(jsonPath("$.shareId").value(SHARE_ID))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 400 POOL_INVALID_AMOUNT when the amount is invalid`() {
        every { selfJoinPoolShareUseCase.selfJoin(any()) } returns SelfJoinPoolShareResult.InvalidAmount

        performSelfJoin().andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("POOL_INVALID_AMOUNT"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 POOL_NOT_FOUND for an unknown global token`() {
        every { selfJoinPoolShareUseCase.selfJoin(any()) } returns SelfJoinPoolShareResult.PoolNotFound

        performSelfJoin().andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("POOL_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 POOL_PAYER_NOT_FOUND when the payer is unknown`() {
        every { selfJoinPoolShareUseCase.selfJoin(any()) } returns SelfJoinPoolShareResult.PayerNotFound

        performSelfJoin().andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("POOL_PAYER_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 POOL_CLOSED when the pool is closed`() {
        every { selfJoinPoolShareUseCase.selfJoin(any()) } returns SelfJoinPoolShareResult.PoolClosed

        performSelfJoin().andExpect(status().isConflict).andExpect(jsonPath("$.code").value("POOL_CLOSED"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 POOL_EXPIRED when the pool deadline passed`() {
        every { selfJoinPoolShareUseCase.selfJoin(any()) } returns SelfJoinPoolShareResult.PoolExpired

        performSelfJoin().andExpect(status().isConflict).andExpect(jsonPath("$.code").value("POOL_EXPIRED"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 POOL_ALREADY_JOINED when the caller already has a share`() {
        every { selfJoinPoolShareUseCase.selfJoin(any()) } returns SelfJoinPoolShareResult.AlreadyJoined

        performSelfJoin().andExpect(status().isConflict).andExpect(jsonPath("$.code").value("POOL_ALREADY_JOINED"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 POOL_REMAINDER_LOCKED when the creator remainder is locked`() {
        every { selfJoinPoolShareUseCase.selfJoin(any()) } returns SelfJoinPoolShareResult.RemainderLocked

        performSelfJoin().andExpect(status().isConflict).andExpect(jsonPath("$.code").value("POOL_REMAINDER_LOCKED"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 POOL_NO_CREATOR_REMAINDER when no creator remainder exists`() {
        every { selfJoinPoolShareUseCase.selfJoin(any()) } returns SelfJoinPoolShareResult.NoCreatorRemainder

        performSelfJoin().andExpect(status().isConflict).andExpect(jsonPath("$.code").value("POOL_NO_CREATOR_REMAINDER"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 POOL_INSUFFICIENT_REMAINDER when the remainder is insufficient`() {
        every { selfJoinPoolShareUseCase.selfJoin(any()) } returns SelfJoinPoolShareResult.InsufficientRemainder

        performSelfJoin().andExpect(status().isConflict).andExpect(jsonPath("$.code").value("POOL_INSUFFICIENT_REMAINDER"))
    }
}
