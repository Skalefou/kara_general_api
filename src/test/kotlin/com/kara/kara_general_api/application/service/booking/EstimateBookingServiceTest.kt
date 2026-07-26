package com.kara.kara_general_api.application.service.booking

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOption
import com.kara.kara_general_api.domain.model.room.RoomOptionId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingCommand
import com.kara.kara_general_api.domain.port.input.booking.EstimateBookingResult
import com.kara.kara_general_api.domain.port.output.RoomOptionRepository
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EstimateBookingServiceTest {
    private val roomRepository = mockk<RoomRepository>()
    private val roomOptionRepository = mockk<RoomOptionRepository>()
    private val sut = EstimateBookingService(roomRepository, roomOptionRepository)

    private val roomId = RoomId(UUID.randomUUID())
    private val start = Instant.parse("2026-08-01T18:00:00Z")

    private val room =
        Room(
            id = roomId,
            name = "Salle Étoile",
            description = "Grande salle lumineuse",
            address = Address(street = "12 rue de la Paix", city = "Paris", postalCode = "75002", country = "France"),
            pricePerPersonPerHour = BigDecimal("12.50"),
            currency = Currency.EUR,
            maxCapacity = 50,
            isThereWifi = true,
            isThereSonoPro = false,
            isThereAirConditioning = true,
            createdAt = Instant.now(),
        )

    @Test
    fun `should return RoomNotFound and skip loading options when room does not exist`() {
        every { roomRepository.findById(roomId) } returns null

        val result = sut.estimate(command(optionIds = emptyList()))

        assertEquals(EstimateBookingResult.RoomNotFound, result)
        verify(exactly = 0) { roomOptionRepository.findByRoomId(any()) }
    }

    @Test
    fun `should load room and options then compute the estimate`() {
        val optionId = RoomOptionId(UUID.randomUUID())
        every { roomRepository.findById(roomId) } returns room
        every { roomOptionRepository.findByRoomId(roomId) } returns
            listOf(
                RoomOption(
                    id = optionId,
                    roomId = roomId,
                    label = "Ménage fin de soirée",
                    description = null,
                    price = BigDecimal("60.00"),
                    currency = Currency.EUR,
                ),
            )

        // 12.50 * 4 * 2h = 100.00 ; option 60.00 ; total 160.00 ; 160/4 = 40.00
        val result = sut.estimate(command(optionIds = listOf(optionId), numberOfPeople = 4))

        val success = assertIs<EstimateBookingResult.Success>(result)
        assertEquals(BigDecimal("100.00"), success.estimate.base)
        assertEquals(BigDecimal("60.00"), success.estimate.optionsTotal)
        assertEquals(BigDecimal("160.00"), success.estimate.totalPrice)
        assertEquals(BigDecimal("40.00"), success.estimate.pricePerPerson)
    }

    private fun command(
        optionIds: List<RoomOptionId>,
        numberOfPeople: Int = 4,
    ): EstimateBookingCommand =
        EstimateBookingCommand(
            roomId = roomId,
            startAt = start,
            endAt = start.plusSeconds(2 * 3600),
            numberOfPeople = numberOfPeople,
            optionIds = optionIds,
        )
}
