package com.kara.kara_general_api.infrastructure.adapter.input.rest.pool

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
import com.kara.kara_general_api.domain.port.input.pool.PoolSummaryView
import com.kara.kara_general_api.domain.port.input.pool.PoolView
import com.kara.kara_general_api.domain.port.input.pool.RegeneratePoolLinkUseCase
import com.kara.kara_general_api.domain.port.input.pool.RemindPoolShareUseCase
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
            shares = emptyList(),
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

        mockMvc.perform(get("/api/v1/pools"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].poolId").value(POOL_ID))
            .andExpect(jsonPath("$[0].roomName").value("Salle Étoile"))
            .andExpect(jsonPath("$[0].isCreator").value(true))
            .andExpect(jsonPath("$[0].percentage").value(40))
    }

    @Test
    fun `should return 401 when creating a pool without authentication`() {
        mockMvc.perform(
            post("/api/v1/pools").contentType(MediaType.APPLICATION_JSON).content(createBody),
        ).andExpect(status().isUnauthorized)

        verify(exactly = 0) { createPoolUseCase.create(any()) }
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 201 with the pool when created`() {
        every { createPoolUseCase.create(any()) } returns CreatePoolResult.Created(poolView())

        mockMvc.perform(
            post("/api/v1/pools").contentType(MediaType.APPLICATION_JSON).content(createBody),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.poolId").value(POOL_ID))
            .andExpect(jsonPath("$.globalLinkToken").value("global-token"))
            .andExpect(jsonPath("$.percentage").value(40))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 with POOL_SHARES_MISMATCH when shares do not sum to the target`() {
        every { createPoolUseCase.create(any()) } returns
            CreatePoolResult.SharesMismatch(expected = BigDecimal("100.00"), actual = BigDecimal("90.00"))

        mockMvc.perform(
            post("/api/v1/pools").contentType(MediaType.APPLICATION_JSON).content(createBody),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("POOL_SHARES_MISMATCH"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 403 when reading a pool the caller does not own`() {
        every { getPoolUseCase.getById(any(), any()) } returns GetPoolResult.NotOwner

        mockMvc.perform(get("/api/v1/pools/$POOL_ID"))
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

        mockMvc.perform(post("/api/v1/pools/$POOL_ID/shares/$SHARE_ID/payment"))
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

        mockMvc.perform(get("/api/v1/pools/join/global-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.booking.roomName").value("Salle Étoile"))
            .andExpect(jsonPath("$.message").isNotEmpty)
    }

    @Test
    fun `should return 404 for an unknown share recap`() {
        every { getPoolRecapUseCase.getByShareToken("nope") } returns GetPoolRecapResult.NotFound

        mockMvc.perform(get("/api/v1/pools/share/nope"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("POOL_NOT_FOUND"))
    }
}
