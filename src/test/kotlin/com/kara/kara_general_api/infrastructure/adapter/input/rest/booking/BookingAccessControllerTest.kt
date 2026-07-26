package com.kara.kara_general_api.infrastructure.adapter.input.rest.booking

import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.port.input.booking.BookingAccessView
import com.kara.kara_general_api.domain.port.input.booking.ValidateBookingAccessResult
import com.kara.kara_general_api.domain.port.input.booking.ValidateBookingAccessUseCase
import com.kara.kara_general_api.infrastructure.config.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

private const val SERVER_ID = "11111111-2222-3333-4444-555555555555"
private const val BOOKING_ID = "99999999-8888-7777-6666-555555555555"

@WebMvcTest(BookingAccessController::class)
@Import(SecurityConfig::class)
class BookingAccessControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var validateBookingAccessUseCase: ValidateBookingAccessUseCase

    private fun view(status: BookingStatus = BookingStatus.CONFIRMED) =
        BookingAccessView(
            bookingId = UUID.fromString(BOOKING_ID),
            ticketCode = "KARA-TKT-3F7Q2K9A",
            clientName = "Alice Martin",
            roomName = "Salle Étoile",
            startAt = Instant.parse("2026-08-01T19:00:00Z"),
            endAt = Instant.parse("2026-08-01T23:00:00Z"),
            numberOfPeople = 6,
            status = status,
        )

    private fun performValidate() = mockMvc.perform(post("/api/v1/bookings/$BOOKING_ID/validate-access"))

    @Test
    fun `should return 401 without authentication`() {
        performValidate().andExpect(status().isUnauthorized)

        verify(exactly = 0) { validateBookingAccessUseCase.validate(any()) }
    }

    @Test
    @WithMockUser(username = SERVER_ID, roles = ["CLIENT"])
    fun `should return 403 for a client`() {
        performValidate().andExpect(status().isForbidden)

        verify(exactly = 0) { validateBookingAccessUseCase.validate(any()) }
    }

    @Test
    @WithMockUser(username = SERVER_ID, roles = ["SERVER"])
    fun `should return 200 with the booking recap when access is granted`() {
        every { validateBookingAccessUseCase.validate(any()) } returns
            ValidateBookingAccessResult.Granted(view(), Instant.parse("2026-08-01T18:55:00Z"))

        performValidate()
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.granted").value(true))
            .andExpect(jsonPath("$.ticketCode").value("KARA-TKT-3F7Q2K9A"))
            .andExpect(jsonPath("$.clientName").value("Alice Martin"))
            .andExpect(jsonPath("$.roomName").value("Salle Étoile"))
            .andExpect(jsonPath("$.numberOfPeople").value(6))
            .andExpect(jsonPath("$.alreadyCheckedIn").value(false))
            .andExpect(jsonPath("$.startAt").value("2026-08-01T19:00:00Z"))
            .andExpect(jsonPath("$.endAt").value("2026-08-01T23:00:00Z"))
            .andExpect(jsonPath("$.checkedInAt").value("2026-08-01T18:55:00Z"))
    }

    @Test
    @WithMockUser(username = SERVER_ID, roles = ["SERVER"])
    fun `should return 409 with the original check-in when the ticket was already validated`() {
        every { validateBookingAccessUseCase.validate(any()) } returns
            ValidateBookingAccessResult.AlreadyCheckedIn(
                view(),
                Instant.parse("2026-08-01T18:40:00Z"),
                "Bob Durand",
            )

        performValidate()
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("BOOKING_ALREADY_CHECKED_IN"))
            .andExpect(jsonPath("$.booking.alreadyCheckedIn").value(true))
            .andExpect(jsonPath("$.booking.checkedInByName").value("Bob Durand"))
    }

    @Test
    @WithMockUser(username = SERVER_ID, roles = ["SERVER"])
    fun `should return 409 when the booking is not confirmed`() {
        every { validateBookingAccessUseCase.validate(any()) } returns
            ValidateBookingAccessResult.NotConfirmed(view(status = BookingStatus.PENDING))

        performValidate()
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("BOOKING_NOT_CONFIRMED"))
            .andExpect(jsonPath("$.booking.status").value("PENDING"))
    }

    @Test
    @WithMockUser(username = SERVER_ID, roles = ["SERVER"])
    fun `should return 409 outside the admission window`() {
        every { validateBookingAccessUseCase.validate(any()) } returns
            ValidateBookingAccessResult.OutsideAdmissionWindow(view())

        performValidate()
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("OUTSIDE_ADMISSION_WINDOW"))
    }

    @Test
    @WithMockUser(username = SERVER_ID, roles = ["SERVER"])
    fun `should return 403 when the server is not assigned to the room`() {
        every { validateBookingAccessUseCase.validate(any()) } returns
            ValidateBookingAccessResult.NotAssignedServer

        performValidate()
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("NOT_ASSIGNED_SERVER"))
    }

    @Test
    @WithMockUser(username = SERVER_ID, roles = ["SERVER"])
    fun `should return 404 when the booking does not exist`() {
        every { validateBookingAccessUseCase.validate(any()) } returns
            ValidateBookingAccessResult.BookingNotFound

        performValidate()
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"))
    }

    @Test
    @WithMockUser(username = SERVER_ID, roles = ["ADMIN"])
    fun `should let an admin validate a ticket`() {
        every { validateBookingAccessUseCase.validate(any()) } returns
            ValidateBookingAccessResult.Granted(view(), Instant.parse("2026-08-01T18:55:00Z"))

        performValidate().andExpect(status().isOk)
    }
}
