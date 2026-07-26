package com.kara.kara_general_api.application.service.servershift

import com.kara.kara_general_api.domain.model.servershift.ServerShiftId
import com.kara.kara_general_api.domain.port.input.servershift.DeleteServerShiftResult
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class DeleteServerShiftServiceTest {
    private val serverShiftRepository = mockk<ServerShiftRepository>()
    private val sut = DeleteServerShiftService(serverShiftRepository)

    private val shiftId = ServerShiftId(UUID.randomUUID())

    @Test
    fun `should return Success when a row is deleted`() {
        every { serverShiftRepository.deleteById(shiftId) } returns true

        assertEquals(DeleteServerShiftResult.Success, sut.deleteServerShift(shiftId))
    }

    @Test
    fun `should return NotFound when no row is deleted`() {
        every { serverShiftRepository.deleteById(shiftId) } returns false

        assertEquals(DeleteServerShiftResult.NotFound, sut.deleteServerShift(shiftId))
    }
}
