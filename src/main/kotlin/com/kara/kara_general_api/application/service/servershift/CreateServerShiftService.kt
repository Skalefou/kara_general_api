package com.kara.kara_general_api.application.service.servershift

import com.kara.kara_general_api.domain.model.servershift.ServerShift
import com.kara.kara_general_api.domain.model.user.UserRole
import com.kara.kara_general_api.domain.port.input.servershift.CreateServerShiftCommand
import com.kara.kara_general_api.domain.port.input.servershift.CreateServerShiftResult
import com.kara.kara_general_api.domain.port.input.servershift.CreateServerShiftUseCase
import com.kara.kara_general_api.domain.port.output.RoomRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import com.kara.kara_general_api.domain.port.output.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Crée un créneau d'agenda pour un serveur. Vérifie l'existence et le rôle du serveur, l'existence de la
 * salle, la validité du créneau ([endAt] > [startAt]) puis l'absence de chevauchement avec un autre
 * créneau du même serveur avant de persister.
 */
@Service
class CreateServerShiftService(
    private val userRepository: UserRepository,
    private val roomRepository: RoomRepository,
    private val serverShiftRepository: ServerShiftRepository,
) : CreateServerShiftUseCase {

    @Transactional
    override fun createServerShift(command: CreateServerShiftCommand): CreateServerShiftResult {
        val server = userRepository.findById(command.serverId) ?: return CreateServerShiftResult.ServerNotFound
        if (server.role != UserRole.SERVER) return CreateServerShiftResult.NotAServer
        if (roomRepository.findById(command.roomId) == null) return CreateServerShiftResult.RoomNotFound
        if (!command.endAt.isAfter(command.startAt)) return CreateServerShiftResult.InvalidTimeSlot

        if (serverShiftRepository.existsOverlappingForServer(command.serverId, command.startAt, command.endAt, null)) {
            return CreateServerShiftResult.SlotUnavailable
        }

        val shift =
            ServerShift.create(
                serverId = command.serverId,
                roomId = command.roomId,
                startAt = command.startAt,
                endAt = command.endAt,
                note = command.note,
            )
        return CreateServerShiftResult.Created(serverShiftRepository.save(shift))
    }
}
