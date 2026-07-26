package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.room.vo.Coordinates
import com.kara.kara_general_api.domain.model.service.ServiceId
import com.kara.kara_general_api.domain.port.input.room.CreateRoomCommand
import com.kara.kara_general_api.domain.port.input.room.CreateRoomResult
import com.kara.kara_general_api.domain.port.output.GeocodingPort
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.RoomServiceRepository
import com.kara.kara_general_api.domain.port.output.ServiceRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreateRoomServiceTest {
    private val roomRepository = mockk<RoomRepository>()
    private val geocodingPort = mockk<GeocodingPort>()
    private val serviceRepository = mockk<ServiceRepository>()
    private val roomServiceRepository = mockk<RoomServiceRepository>(relaxed = true)
    private val sut = CreateRoomService(roomRepository, geocodingPort, serviceRepository, roomServiceRepository)

    private val address = Address(street = "12 rue de la Paix", city = "Paris", postalCode = "75002", country = "France")
    private val command =
        CreateRoomCommand(
            name = "Salle Étoile",
            description = "Grande salle lumineuse",
            address = address,
            pricePerPersonPerHour = BigDecimal("12.50"),
            currency = Currency.EUR,
            maxCapacity = 50,
            isThereWifi = true,
            isThereSonoPro = false,
            isThereAirConditioning = true,
        )

    @Test
    fun `should geocode address then create and persist room with coordinates`() {
        every { geocodingPort.geocode(address) } returns Coordinates(latitude = 48.8566, longitude = 2.3522)
        val savedRoom = slot<Room>()
        every { roomRepository.save(capture(savedRoom)) } answers { savedRoom.captured }

        val result = sut.createRoom(command)

        val success = assertIs<CreateRoomResult.Success>(result)
        assertEquals("Salle Étoile", success.room.name)
        assertEquals(address, success.room.address)
        assertEquals(48.8566, success.room.latitude)
        assertEquals(2.3522, success.room.longitude)
        verify { roomRepository.save(success.room) }
    }

    @Test
    fun `should return AddressNotFound and not persist when geocoding yields no result`() {
        every { geocodingPort.geocode(address) } returns null

        val result = sut.createRoom(command)

        assertEquals(CreateRoomResult.AddressNotFound, result)
        verify(exactly = 0) { roomRepository.save(any()) }
    }

    @Test
    fun `should create service links for existing services after saving the room`() {
        val serviceId = ServiceId(UUID.randomUUID())
        every { serviceRepository.existsById(serviceId) } returns true
        every { geocodingPort.geocode(address) } returns Coordinates(latitude = 48.8566, longitude = 2.3522)
        val savedRoom = slot<Room>()
        every { roomRepository.save(capture(savedRoom)) } answers { savedRoom.captured }
        every { roomServiceRepository.addLinks(any(), any()) } just runs

        val result = sut.createRoom(command.copy(serviceIds = listOf(serviceId)))

        val success = assertIs<CreateRoomResult.Success>(result)
        verify { roomServiceRepository.addLinks(success.room.id, listOf(serviceId)) }
    }

    @Test
    fun `should return UnknownService and persist nothing when a service does not exist`() {
        val serviceId = ServiceId(UUID.randomUUID())
        every { serviceRepository.existsById(serviceId) } returns false

        val result = sut.createRoom(command.copy(serviceIds = listOf(serviceId)))

        assertEquals(CreateRoomResult.UnknownService(serviceId), result)
        verify(exactly = 0) { roomRepository.save(any()) }
        verify(exactly = 0) { geocodingPort.geocode(any()) }
        verify(exactly = 0) { roomServiceRepository.addLinks(any(), any()) }
    }
}
