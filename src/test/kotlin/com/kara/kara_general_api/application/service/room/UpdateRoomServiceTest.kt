package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomStatus
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.room.vo.Coordinates
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomCommand
import com.kara.kara_general_api.domain.port.input.room.UpdateRoomResult
import com.kara.kara_general_api.domain.port.output.GeocodingPort
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpdateRoomServiceTest {

    private val roomRepository = mockk<RoomRepository>()
    private val geocodingPort = mockk<GeocodingPort>()
    private val sut = UpdateRoomService(roomRepository, geocodingPort)

    private val roomId = RoomId(UUID.randomUUID())
    private val existingRoom =
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
            latitude = 48.8566,
            longitude = 2.3522,
        )
    private val command =
        UpdateRoomCommand(
            id = roomId,
            name = "Salle Lune",
            description = null,
            street = "5 avenue Foch",
            city = "Lyon",
            postalCode = "69000",
            country = "France",
            pricePerPersonPerHour = null,
            currency = null,
            maxCapacity = null,
            isThereWifi = null,
            isThereSonoPro = null,
            isThereAirConditioning = null,
            status = null,
        )

    @Test
    fun `should geocode new address then update and persist room`() {
        every { roomRepository.findById(roomId) } returns existingRoom
        every { geocodingPort.geocode(any()) } returns Coordinates(latitude = 45.75, longitude = 4.85)
        val savedRoom = slot<Room>()
        every { roomRepository.save(capture(savedRoom)) } answers { savedRoom.captured }

        val result = sut.updateRoom(command)

        val success = assertIs<UpdateRoomResult.Success>(result)
        assertEquals("Salle Lune", success.room.name)
        assertEquals(
            Address(street = "5 avenue Foch", city = "Lyon", postalCode = "69000", country = "France"),
            success.room.address,
        )
        assertEquals(45.75, success.room.latitude)
        assertEquals(4.85, success.room.longitude)
        assertEquals(roomId, success.room.id)
        verify(exactly = 1) { geocodingPort.geocode(any()) }
    }

    @Test
    fun `should keep existing coordinates and skip geocoding when address is unchanged`() {
        every { roomRepository.findById(roomId) } returns existingRoom
        val savedRoom = slot<Room>()
        every { roomRepository.save(capture(savedRoom)) } answers { savedRoom.captured }

        val partialCommand =
            UpdateRoomCommand(
                id = roomId,
                name = "Salle Lune",
                description = null,
                street = null,
                city = null,
                postalCode = null,
                country = null,
                pricePerPersonPerHour = null,
                currency = null,
                maxCapacity = null,
                isThereWifi = null,
                isThereSonoPro = null,
                isThereAirConditioning = null,
                status = null,
            )

        val result = sut.updateRoom(partialCommand)

        val success = assertIs<UpdateRoomResult.Success>(result)
        assertEquals("Salle Lune", success.room.name)
        assertEquals(existingRoom.address, success.room.address)
        assertEquals(existingRoom.status, success.room.status)
        assertEquals(48.8566, success.room.latitude)
        assertEquals(2.3522, success.room.longitude)
        verify(exactly = 0) { geocodingPort.geocode(any()) }
    }

    @Test
    fun `should close room when status is CLOSED without geocoding`() {
        every { roomRepository.findById(roomId) } returns existingRoom
        val savedRoom = slot<Room>()
        every { roomRepository.save(capture(savedRoom)) } answers { savedRoom.captured }

        val closeCommand =
            UpdateRoomCommand(
                id = roomId,
                name = null,
                description = null,
                street = null,
                city = null,
                postalCode = null,
                country = null,
                pricePerPersonPerHour = null,
                currency = null,
                maxCapacity = null,
                isThereWifi = null,
                isThereSonoPro = null,
                isThereAirConditioning = null,
                status = RoomStatus.CLOSED,
            )

        val result = sut.updateRoom(closeCommand)

        val success = assertIs<UpdateRoomResult.Success>(result)
        assertEquals(RoomStatus.CLOSED, success.room.status)
        assertEquals(existingRoom.name, success.room.name)
        assertEquals(existingRoom.address, success.room.address)
        verify(exactly = 0) { geocodingPort.geocode(any()) }
    }

    @Test
    fun `should return AddressNotFound and not persist when new address is not geocodable`() {
        every { roomRepository.findById(roomId) } returns existingRoom
        every { geocodingPort.geocode(any()) } returns null

        val result = sut.updateRoom(command)

        assertEquals(UpdateRoomResult.AddressNotFound, result)
        verify(exactly = 0) { roomRepository.save(any()) }
    }

    @Test
    fun `should return NotFound when room does not exist`() {
        every { roomRepository.findById(roomId) } returns null

        val result = sut.updateRoom(command)

        assertEquals(UpdateRoomResult.NotFound, result)
    }
}
