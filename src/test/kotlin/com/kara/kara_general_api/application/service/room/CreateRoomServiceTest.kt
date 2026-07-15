package com.kara.kara_general_api.application.service.room

import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.room.vo.Coordinates
import com.kara.kara_general_api.domain.port.input.room.CreateRoomCommand
import com.kara.kara_general_api.domain.port.input.room.CreateRoomResult
import com.kara.kara_general_api.domain.port.output.GeocodingPort
import com.kara.kara_general_api.domain.port.output.RoomRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreateRoomServiceTest {

    private val roomRepository = mockk<RoomRepository>()
    private val geocodingPort = mockk<GeocodingPort>()
    private val sut = CreateRoomService(roomRepository, geocodingPort)

    private val address = Address(street = "12 rue de la Paix", city = "Paris", postalCode = "75002", country = "France")
    private val command =
        CreateRoomCommand(
            name = "Salle Étoile",
            address = address,
            pricePerPersonPerHour = BigDecimal("12.50"),
            currency = Currency.EUR,
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
}
