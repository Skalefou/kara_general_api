package com.kara.kara_general_api.application.service.servershift

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.servershift.ServerShift
import com.kara.kara_general_api.domain.model.servershift.ServerShiftId
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.servershift.ListServerShiftsQuery
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class ListServerShiftsServiceTest {
    private val serverShiftRepository = mockk<ServerShiftRepository>()
    private val sut = ListServerShiftsService(serverShiftRepository)

    @Test
    fun `should forward the query filters to the repository`() {
        val serverId = UserId(UUID.randomUUID())
        val roomId = RoomId(UUID.randomUUID())
        val from = Instant.parse("2026-08-01T00:00:00Z")
        val to = Instant.parse("2026-08-31T00:00:00Z")
        val shift =
            ServerShift(
                id = ServerShiftId(UUID.randomUUID()),
                serverId = serverId,
                roomId = roomId,
                startAt = from.plusSeconds(3600),
                endAt = from.plusSeconds(7200),
                note = null,
                createdAt = Instant.now(),
            )
        every { serverShiftRepository.findAll(serverId, roomId, from, to) } returns listOf(shift)

        val result = sut.listServerShifts(ListServerShiftsQuery(serverId = serverId, roomId = roomId, from = from, to = to))

        assertEquals(listOf(shift), result)
        verify { serverShiftRepository.findAll(serverId, roomId, from, to) }
    }

    @Test
    fun `should list everything when no filter is provided`() {
        every { serverShiftRepository.findAll(null, null, null, null) } returns emptyList()

        val result = sut.listServerShifts(ListServerShiftsQuery())

        assertEquals(emptyList(), result)
    }
}
