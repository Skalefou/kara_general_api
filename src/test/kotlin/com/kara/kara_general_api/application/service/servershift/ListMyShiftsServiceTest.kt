package com.kara.kara_general_api.application.service.servershift

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.servershift.ServerShift
import com.kara.kara_general_api.domain.model.servershift.ServerShiftId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class ListMyShiftsServiceTest {
    private val serverShiftRepository = mockk<ServerShiftRepository>()
    private val roomRepository = mockk<RoomRepository>()
    private val sut = ListMyShiftsService(serverShiftRepository, roomRepository)

    private val serverId = UserId(UUID.randomUUID())
    private val roomId = RoomId(UUID.randomUUID())

    @Test
    fun `should enrich shifts with room name and city`() {
        val shift =
            ServerShift(
                id = ServerShiftId(UUID.randomUUID()),
                serverId = serverId,
                roomId = roomId,
                startAt = Instant.parse("2026-08-01T18:00:00Z"),
                endAt = Instant.parse("2026-08-01T23:00:00Z"),
                note = "Bar",
                createdAt = Instant.now(),
            )
        every { serverShiftRepository.findAll(serverId, null, null, null) } returns listOf(shift)
        every { roomRepository.findById(roomId) } returns
            Room(
                id = roomId,
                name = "Salle Étoile",
                description = "desc",
                address = Address(street = "1 rue", city = "Paris", postalCode = "75001", country = "France"),
                pricePerPersonPerHour = BigDecimal("10.00"),
                currency = Currency.EUR,
                maxCapacity = 20,
                isThereWifi = true,
                isThereSonoPro = false,
                isThereAirConditioning = false,
                createdAt = Instant.now(),
            )

        val result = sut.listMyShifts(serverId)

        assertEquals(1, result.size)
        assertEquals("Salle Étoile", result.first().roomName)
        assertEquals("Paris", result.first().roomCity)
        assertEquals(shift, result.first().shift)
    }

    @Test
    fun `should fall back to placeholder when the room is missing`() {
        val shift =
            ServerShift(
                id = ServerShiftId(UUID.randomUUID()),
                serverId = serverId,
                roomId = roomId,
                startAt = Instant.parse("2026-08-01T18:00:00Z"),
                endAt = Instant.parse("2026-08-01T23:00:00Z"),
                note = null,
                createdAt = Instant.now(),
            )
        every { serverShiftRepository.findAll(serverId, null, null, null) } returns listOf(shift)
        every { roomRepository.findById(roomId) } returns null

        val result = sut.listMyShifts(serverId)

        assertEquals("Salle inconnue", result.first().roomName)
        assertEquals("", result.first().roomCity)
    }
}
