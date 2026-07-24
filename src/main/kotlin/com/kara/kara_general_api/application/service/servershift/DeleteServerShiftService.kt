package com.kara.kara_general_api.application.service.servershift

import com.kara.kara_general_api.domain.model.servershift.ServerShiftId
import com.kara.kara_general_api.domain.port.input.servershift.DeleteServerShiftResult
import com.kara.kara_general_api.domain.port.input.servershift.DeleteServerShiftUseCase
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteServerShiftService(
    private val serverShiftRepository: ServerShiftRepository,
) : DeleteServerShiftUseCase {

    @Transactional
    override fun deleteServerShift(id: ServerShiftId): DeleteServerShiftResult =
        if (serverShiftRepository.deleteById(id)) {
            DeleteServerShiftResult.Success
        } else {
            DeleteServerShiftResult.NotFound
        }
}
