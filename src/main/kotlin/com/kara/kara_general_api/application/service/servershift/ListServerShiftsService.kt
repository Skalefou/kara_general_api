package com.kara.kara_general_api.application.service.servershift

import com.kara.kara_general_api.domain.model.servershift.ServerShift
import com.kara.kara_general_api.domain.port.input.servershift.ListServerShiftsQuery
import com.kara.kara_general_api.domain.port.input.servershift.ListServerShiftsUseCase
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import org.springframework.stereotype.Service

@Service
class ListServerShiftsService(
    private val serverShiftRepository: ServerShiftRepository,
) : ListServerShiftsUseCase {
    override fun listServerShifts(query: ListServerShiftsQuery): List<ServerShift> =
        serverShiftRepository.findAll(
            serverId = query.serverId,
            roomId = query.roomId,
            from = query.from,
            to = query.to,
        )
}
