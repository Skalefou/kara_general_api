package com.kara.kara_general_api.domain.port.input.servershift

import com.kara.kara_general_api.domain.model.servershift.ServerShiftId

sealed interface DeleteServerShiftResult {
    data object Success : DeleteServerShiftResult

    data object NotFound : DeleteServerShiftResult
}

interface DeleteServerShiftUseCase {
    fun deleteServerShift(id: ServerShiftId): DeleteServerShiftResult
}
