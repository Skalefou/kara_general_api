package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingEstimate
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.booking.PaymentMode
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.payment.PoolStatus
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.booking.BookingDetailView
import com.kara.kara_general_api.domain.port.input.booking.CancelBookingResult
import com.kara.kara_general_api.domain.port.input.booking.CancelBookingUseCase
import com.kara.kara_general_api.domain.port.input.booking.CreateBookingResult
import com.kara.kara_general_api.domain.port.input.booking.CreateBookingUseCase
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingResult
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingUseCase
import com.kara.kara_general_api.domain.port.input.booking.GetBookingDetailResult
import com.kara.kara_general_api.domain.port.input.booking.GetBookingDetailUseCase
import com.kara.kara_general_api.domain.port.input.booking.ListAllBookingsUseCase
import com.kara.kara_general_api.domain.port.input.booking.ListServerBookingsUseCase
import com.kara.kara_general_api.domain.port.input.booking.ListUserBookingsResult
import com.kara.kara_general_api.domain.port.input.booking.ListUserBookingsUseCase
import com.kara.kara_general_api.domain.port.input.booking.TriggerEmergencyUseCase
import com.kara.kara_general_api.domain.port.input.booking.UserBookingOptionView
import com.kara.kara_general_api.domain.port.input.booking.UserBookingPoolShareView
import com.kara.kara_general_api.domain.port.input.booking.UserBookingPoolView
import com.kara.kara_general_api.domain.port.input.booking.UserBookingView
import com.kara.kara_general_api.domain.port.input.chat.OpenBookingConversationUseCase
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

private const val ROOM_ID = "550e8400-e29b-41d4-a716-446655440000"
private const val USER_ID = "11111111-2222-3333-4444-555555555555"
private const val BOOKING_ID = "99999999-8888-7777-6666-555555555555"
private const val OPTION_ID = "c0ffee00-0000-4000-8000-000000000001"
private const val POOL_ID = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"
private const val SHARE_ID = "11111111-1111-4111-8111-111111111111"
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
    private lateinit var listUserBookingsUseCase: ListUserBookingsUseCase

    @MockkBean
    private lateinit var listAllBookingsUseCase: ListAllBookingsUseCase

    @MockkBean
    private lateinit var triggerEmergencyUseCase: TriggerEmergencyUseCase

    @MockkBean
    private lateinit var getBookingDetailUseCase: GetBookingDetailUseCase

    @MockkBean
    private lateinit var cancelBookingUseCase: CancelBookingUseCase

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
    fun `should return 400 when the estimated slot is shorter than one hour`() {
        every { estimateBookingUseCase.estimate(any()) } returns EstimateBookingResult.DurationTooShort(60)

        perform()
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BOOKING_DURATION_TOO_SHORT"))
            .andExpect(jsonPath("$.detail").isNotEmpty)
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 400 when the booked slot is shorter than one hour`() {
        every { createBookingUseCase.createBooking(any()) } returns CreateBookingResult.DurationTooShort(60)

        performCreate()
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("BOOKING_DURATION_TOO_SHORT"))
            .andExpect(jsonPath("$.detail").isNotEmpty)
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

        mockMvc
            .perform(
                post("/api/v1/bookings/estimate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))

        verify(exactly = 0) { estimateBookingUseCase.estimate(any()) }
    }

    private fun detailView(
        status: BookingStatus,
        ticketCode: String?,
    ) = BookingDetailView(
        bookingId = UUID.fromString(BOOKING_ID),
        roomName = "Salle Étoile",
        roomAddress = "12 rue de Paris, 69002 Lyon, France",
        roomLatitude = 45.7578,
        roomLongitude = 4.8320,
        startAt = Instant.parse("2026-08-01T18:00:00Z"),
        endAt = Instant.parse("2026-08-01T21:00:00Z"),
        numberOfPeople = 8,
        totalPrice = BigDecimal("435.00"),
        currency = Currency.EUR,
        status = status,
        paymentMode = PaymentMode.PAY_ALL,
        ticketCode = ticketCode,
    )

    @Test
    fun `should return 401 when fetching a booking detail without authentication`() {
        mockMvc.perform(get("/api/v1/bookings/$BOOKING_ID")).andExpect(status().isUnauthorized)

        verify(exactly = 0) { getBookingDetailUseCase.getDetail(any(), any()) }
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with the ticket code when the booking is confirmed`() {
        every { getBookingDetailUseCase.getDetail(any(), any()) } returns
            GetBookingDetailResult.Found(detailView(BookingStatus.CONFIRMED, "KARA-TKT-3F7Q2K9A"))

        mockMvc
            .perform(get("/api/v1/bookings/$BOOKING_ID"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.ticketCode").value("KARA-TKT-3F7Q2K9A"))
            .andExpect(jsonPath("$.roomAddress").value("12 rue de Paris, 69002 Lyon, France"))
            .andExpect(jsonPath("$.roomLatitude").value(45.7578))
            .andExpect(jsonPath("$.roomLongitude").value(4.8320))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with a null ticket code when the booking is not confirmed`() {
        every { getBookingDetailUseCase.getDetail(any(), any()) } returns
            GetBookingDetailResult.Found(detailView(BookingStatus.PENDING, null))

        mockMvc
            .perform(get("/api/v1/bookings/$BOOKING_ID"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.ticketCode").doesNotExist())
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 403 when fetching a booking the user does not own`() {
        every { getBookingDetailUseCase.getDetail(any(), any()) } returns GetBookingDetailResult.NotOwner

        mockMvc
            .perform(get("/api/v1/bookings/$BOOKING_ID"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("BOOKING_NOT_OWNER"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 404 when fetching an unknown booking`() {
        every { getBookingDetailUseCase.getDetail(any(), any()) } returns GetBookingDetailResult.NotFound

        mockMvc
            .perform(get("/api/v1/bookings/$BOOKING_ID"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 with refunded flag when cancelling a confirmed booking`() {
        val confirmed = sampleBooking().copy(status = BookingStatus.CONFIRMED)
        every { cancelBookingUseCase.cancel(any()) } returns
            CancelBookingResult.Cancelled(confirmed.cancel(), refunded = true)

        mockMvc
            .perform(post("/api/v1/bookings/$BOOKING_ID/cancel"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.refunded").value(true))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 200 without refund when cancelling a pending booking`() {
        every { cancelBookingUseCase.cancel(any()) } returns
            CancelBookingResult.Cancelled(sampleBooking().cancel(), refunded = false)

        mockMvc
            .perform(post("/api/v1/bookings/$BOOKING_ID/cancel"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.refunded").value(false))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 when cancelling an already cancelled booking`() {
        every { cancelBookingUseCase.cancel(any()) } returns CancelBookingResult.AlreadyCancelled

        mockMvc
            .perform(post("/api/v1/bookings/$BOOKING_ID/cancel"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("BOOKING_ALREADY_CANCELLED"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 409 when cancelling a booking already started`() {
        every { cancelBookingUseCase.cancel(any()) } returns CancelBookingResult.AlreadyStarted

        mockMvc
            .perform(post("/api/v1/bookings/$BOOKING_ID/cancel"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("BOOKING_ALREADY_STARTED"))
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `should return 403 when cancelling a booking the user does not own`() {
        every { cancelBookingUseCase.cancel(any()) } returns CancelBookingResult.NotOwner

        mockMvc
            .perform(post("/api/v1/bookings/$BOOKING_ID/cancel"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("BOOKING_NOT_OWNER"))
    }

    private fun myBookingView(
        paymentMode: PaymentMode,
        pool: UserBookingPoolView?,
    ) = UserBookingView(
        bookingId = UUID.fromString(BOOKING_ID),
        roomId = UUID.fromString(ROOM_ID),
        roomName = "Salle Étoile",
        roomAddress = "12 rue de Paris, 69002 Lyon, France",
        startAt = Instant.parse("2026-08-01T18:00:00Z"),
        endAt = Instant.parse("2026-08-01T21:00:00Z"),
        status = BookingStatus.CONFIRMED,
        paymentMode = paymentMode,
        numberOfPeople = 8,
        totalPrice = BigDecimal("435.00"),
        currency = Currency.EUR,
        expiresAt = Instant.parse("2026-07-20T10:15:00Z"),
        options =
            listOf(
                UserBookingOptionView(
                    optionId = UUID.fromString(OPTION_ID),
                    label = "Ménage fin de soirée",
                    price = BigDecimal("60.00"),
                    currency = Currency.EUR,
                ),
            ),
        pool = pool,
    )

    private fun myBookingPoolView() =
        UserBookingPoolView(
            poolId = UUID.fromString(POOL_ID),
            status = PoolStatus.OPEN,
            targetAmount = BigDecimal("435.00"),
            collectedAmount = BigDecimal("217.50"),
            currency = Currency.EUR,
            percentage = 50,
            deadline = Instant.parse("2026-08-01T16:00:00Z"),
            shares =
                listOf(
                    UserBookingPoolShareView(
                        shareId = UUID.fromString(SHARE_ID),
                        participantName = "Jeanne Martin",
                        email = "jeanne@example.com",
                        amount = BigDecimal("217.50"),
                        status = PoolShareStatus.AUTHORIZED,
                    ),
                    UserBookingPoolShareView(
                        shareId = UUID.randomUUID(),
                        participantName = "Karim Belkacem",
                        email = null,
                        amount = BigDecimal("217.50"),
                        status = PoolShareStatus.PENDING,
                    ),
                ),
        )

    @Test
    fun `should return 401 when listing my bookings without authentication`() {
        mockMvc.perform(get("/api/v1/bookings/me")).andExpect(status().isUnauthorized)

        verify(exactly = 0) { listUserBookingsUseCase.listForUser(any()) }
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["CLIENT"])
    fun `should return 200 with my bookings for an authenticated client`() {
        every { listUserBookingsUseCase.listForUser(any()) } returns
            ListUserBookingsResult.Success(listOf(myBookingView(PaymentMode.PAY_ALL, pool = null)))

        mockMvc
            .perform(get("/api/v1/bookings/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].bookingId").value(BOOKING_ID))
            .andExpect(jsonPath("$[0].roomId").value(ROOM_ID))
            .andExpect(jsonPath("$[0].roomName").value("Salle Étoile"))
            .andExpect(jsonPath("$[0].roomAddress").value("12 rue de Paris, 69002 Lyon, France"))
            .andExpect(jsonPath("$[0].startAt").value("2026-08-01T18:00:00Z"))
            .andExpect(jsonPath("$[0].endAt").value("2026-08-01T21:00:00Z"))
            .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
            .andExpect(jsonPath("$[0].paymentMode").value("PAY_ALL"))
            .andExpect(jsonPath("$[0].numberOfPeople").value(8))
            .andExpect(jsonPath("$[0].totalPrice").value(435.00))
            .andExpect(jsonPath("$[0].currency").value("EUR"))
            .andExpect(jsonPath("$[0].expiresAt").value("2026-07-20T10:15:00Z"))
            .andExpect(jsonPath("$[0].options[0].optionId").value(OPTION_ID))
            .andExpect(jsonPath("$[0].options[0].label").value("Ménage fin de soirée"))
            .andExpect(jsonPath("$[0].options[0].price").value(60.00))
            .andExpect(jsonPath("$[0].pool").doesNotExist())
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["CLIENT"])
    fun `should expose the participant names when a booking carries a pool`() {
        every { listUserBookingsUseCase.listForUser(any()) } returns
            ListUserBookingsResult.Success(
                listOf(myBookingView(PaymentMode.SHARED_POT, pool = myBookingPoolView())),
            )

        mockMvc
            .perform(get("/api/v1/bookings/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].paymentMode").value("SHARED_POT"))
            .andExpect(jsonPath("$[0].pool.poolId").value(POOL_ID))
            .andExpect(jsonPath("$[0].pool.status").value("OPEN"))
            .andExpect(jsonPath("$[0].pool.targetAmount").value(435.00))
            .andExpect(jsonPath("$[0].pool.collectedAmount").value(217.50))
            .andExpect(jsonPath("$[0].pool.percentage").value(50))
            .andExpect(jsonPath("$[0].pool.deadline").value("2026-08-01T16:00:00Z"))
            .andExpect(jsonPath("$[0].pool.shares.length()").value(2))
            .andExpect(jsonPath("$[0].pool.shares[0].shareId").value(SHARE_ID))
            .andExpect(jsonPath("$[0].pool.shares[0].participantName").value("Jeanne Martin"))
            .andExpect(jsonPath("$[0].pool.shares[0].email").value("jeanne@example.com"))
            .andExpect(jsonPath("$[0].pool.shares[0].amount").value(217.50))
            .andExpect(jsonPath("$[0].pool.shares[0].status").value("AUTHORIZED"))
            .andExpect(jsonPath("$[0].pool.shares[1].participantName").value("Karim Belkacem"))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["CLIENT"])
    fun `should return 200 with an empty array when the client has no booking`() {
        every { listUserBookingsUseCase.listForUser(any()) } returns
            ListUserBookingsResult.Success(emptyList())

        mockMvc
            .perform(get("/api/v1/bookings/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    @WithMockUser(username = USER_ID, roles = ["CLIENT"])
    fun `should forbid a client from listing all the bookings of the platform`() {
        mockMvc.perform(get("/api/v1/bookings")).andExpect(status().isForbidden)

        verify(exactly = 0) { listAllBookingsUseCase.listAllBookings() }
    }
}
