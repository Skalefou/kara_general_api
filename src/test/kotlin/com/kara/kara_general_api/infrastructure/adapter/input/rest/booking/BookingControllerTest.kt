package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingEstimate
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.CreateBookingResult
import com.kara.kara_general_api.domain.port.input.booking.CreateBookingUseCase
import com.kara.kara_general_api.domain.port.input.booking.ListAllBookingsUseCase
import com.kara.kara_general_api.domain.port.input.booking.ListServerBookingsUseCase
import com.kara.kara_general_api.domain.port.input.booking.TriggerEmergencyUseCase
import com.kara.kara_general_api.domain.port.input.chat.OpenBookingConversationUseCase
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingResult
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingUseCase
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

private const val ROOM_ID = "550e8400-e29b-41d4-a716-446655440000"
private const val USER_ID = "11111111-2222-3333-4444-555555555555"
private const val REQUEST_BODY =
    """{"roomId": "$ROOM_ID", "startAt": "2026-08-01T18:00:00Z", "endAt": "2026-08-01T21:30:00Z", """ +
        """"numberOfPeople": 8, "optionIds": []}"""

@WebMvcTest(BookingController::class)
@Import(SecurityConfig::class)
class BookingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var estimateBookingUseCase: EstimateBookingUseCase

    @MockkBean
    private lateinit var createBookingUseCase: CreateBookingUseCase

    @MockkBean
    private lateinit var listServerBookingsUseCase: ListServerBookingsUseCase

    @MockkBean
    private lateinit var openBookingConversationUseCase: OpenBookingConversationUseCase

    @MockkBean
    private lateinit var listAllBookingsUseCase: ListAllBookingsUseCase

    @MockkBean
    private lateinit var triggerEmergencyUseCase: TriggerEmergencyUseCase

    private fun sampleBooking() =
        Booking(
            id = BookingId(UUID.randomUUID()),
            roomId = RoomId(UUID.fromString(ROOM_ID)),
            userId = UserId(UUID.fromString(USER_ID)),
            startAt = Instant.parse("2026-08-01T18:00:00Z"),
            endAt = Instant.parse("2026-08-01T21:30:00Z"),
            numberOfPeople = 8,
            selectedOptionIds = emptyList(),
            totalPrice = BigDecimal("435.00"),
            currency = Currency.EUR,
            status = BookingStatus.PENDING,
            createdAt = Instant.parse("2026-07-20T10:00:00Z"),
            expiresAt = Instant.parse("2026-07-20T10:15:00Z"),
        )

    private fun performCreate() =
        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        )

    @Test
    fun `should return 401 when creating a booking without authentication`() {
        performCreate().andExpect(status().isUnauthorized)

        verify(exactly = 0) { createBookingUseCase.createBooking(any()) }
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 201 with the created booking`() {
        every { createBookingUseCase.createBooking(any()) } returns CreateBookingResult.Created(sampleBooking())

        performCreate()
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.roomId").value(ROOM_ID))
            .andExpect(jsonPath("$.userId").value(USER_ID))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.totalPrice").value(435.00))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.expiresAt").value("2026-07-20T10:15:00Z"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 when the slot is unavailable`() {
        every { createBookingUseCase.createBooking(any()) } returns CreateBookingResult.SlotUnavailable

        performCreate()
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("BOOKING_SLOT_UNAVAILABLE"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when creating a booking on an unknown room`() {
        every { createBookingUseCase.createBooking(any()) } returns CreateBookingResult.RoomNotFound

        performCreate()
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
    }

    private fun perform() =
        mockMvc.perform(
            post("/api/v1/bookings/estimate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY),
        )

    @Test
    fun `should return 200 with the estimate without authentication`() {
        every { estimateBookingUseCase.estimate(any()) } returns
            EstimateBookingResult.Success(
                BookingEstimate(
                    totalPrice = BigDecimal("435.00"),
                    pricePerPerson = BigDecimal("54.38"),
                    currency = Currency.EUR,
                    base = BigDecimal("350.00"),
                    optionsTotal = BigDecimal("85.00"),
                ),
            )

        perform()
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalPrice").value(435.00))
            .andExpect(jsonPath("$.pricePerPerson").value(54.38))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.breakdown.base").value(350.00))
            .andExpect(jsonPath("$.breakdown.options").value(85.00))
    }

    @Test
    fun `should return 404 when the room is not found`() {
        every { estimateBookingUseCase.estimate(any()) } returns EstimateBookingResult.RoomNotFound

        perform()
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
    }

    @Test
    fun `should return 400 when there are fewer than two people`() {
        every { estimateBookingUseCase.estimate(any()) } returns EstimateBookingResult.TooFewPeople

        perform()
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("TOO_FEW_PEOPLE"))
    }

    @Test
    fun `should return 400 when the capacity is exceeded`() {
        every { estimateBookingUseCase.estimate(any()) } returns EstimateBookingResult.CapacityExceeded(50)

        perform()
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CAPACITY_EXCEEDED"))
    }

    @Test
    fun `should return 400 when the time slot is invalid`() {
        every { estimateBookingUseCase.estimate(any()) } returns EstimateBookingResult.InvalidTimeSlot

        perform()
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_TIME_SLOT"))
    }

    @Test
    fun `should return 400 when an option does not belong to the room`() {
        every { estimateBookingUseCase.estimate(any()) } returns
            EstimateBookingResult.UnknownOptions(listOf(UUID.randomUUID()))

        perform()
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("UNKNOWN_ROOM_OPTION"))
    }

    @Test
    fun `should return 400 when the request body fails bean validation`() {
        val invalidBody =
            """{"roomId": "$ROOM_ID", "startAt": "2026-08-01T18:00:00Z", "endAt": "2026-08-01T21:30:00Z", """ +
                """"numberOfPeople": 1, "optionIds": []}"""

        mockMvc.perform(
            post("/api/v1/bookings/estimate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))

        verify(exactly = 0) { estimateBookingUseCase.estimate(any()) }
    }
}
