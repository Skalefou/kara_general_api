package com.kara.kara_general_api.application.service.servershift

import com.kara.kara_general_api.domain.port.input.servershift.UpdateServerShiftCommand
import com.kara.kara_general_api.domain.port.input.servershift.UpdateServerShiftResult
import com.kara.kara_general_api.domain.port.input.servershift.UpdateServerShiftUseCase
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Met à jour un créneau d'agenda existant (salle, bornes horaires, note). Applique les champs fournis,
 * revalide le créneau résultant et l'absence de chevauchement avec les autres créneaux du même serveur
 * (le créneau courant est exclu du contrôle).
 */
@Service
class UpdateServerShiftService(
    private val roomRepository: RoomRepository,
    private val serverShiftRepository: ServerShiftRepository,
) : UpdateServerShiftUseCase {

    @Transactional
    override fun updateServerShift(command: UpdateServerShiftCommand): UpdateServerShiftResult {
        val existing = serverShiftRepository.findById(command.id) ?: return UpdateServerShiftResult.NotFound

        val roomId = command.roomId ?: existing.roomId
        if (command.roomId != null && roomRepository.findById(command.roomId) == null) {
            return UpdateServerShiftResult.RoomNotFound
        }

        val startAt = command.startAt ?: existing.startAt
        val endAt = command.endAt ?: existing.endAt
        if (!endAt.isAfter(startAt)) return UpdateServerShiftResult.InvalidTimeSlot

        if (serverShiftRepository.existsOverlappingForServer(existing.serverId, startAt, endAt, existing.id)) {
            return UpdateServerShiftResult.SlotUnavailable
        }

        val note =
            when {
                command.clearNote -> null
                command.note != null -> command.note.takeIf { it.isNotBlank() }?.trim()
                else -> existing.note
            }

        val updated = existing.copy(roomId = roomId, startAt = startAt, endAt = endAt, note = note)
        return UpdateServerShiftResult.Success(serverShiftRepository.save(updated))
    }
}
