package com.kara.kara_general_api.domain.model.servershift

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant

/**
 * Agrégat Créneau de travail (agenda serveur). Représente l'affectation d'un serveur à une salle sur
 * un créneau [startAt, endAt) : où le serveur doit travailler et quand. Édité exclusivement par un
 * administrateur depuis le back-office. Deux créneaux d'un même serveur ne peuvent pas se chevaucher.
 */
data class ServerShift(
    val id: ServerShiftId,
    val serverId: UserId,
    val roomId: RoomId,
    val startAt: Instant,
    val endAt: Instant,
    val note: String?,
    val createdAt: Instant,
) {
    companion object {
        /** Crée un créneau d'agenda pour un serveur. Le créneau doit être non vide ([endAt] > [startAt]). */
        fun create(
            serverId: UserId,
            roomId: RoomId,
            startAt: Instant,
            endAt: Instant,
            note: String?,
        ): ServerShift =
            ServerShift(
                id = ServerShiftId.generate(),
                serverId = serverId,
                roomId = roomId,
                startAt = startAt,
                endAt = endAt,
                note = note?.takeIf { it.isNotBlank() }?.trim(),
                createdAt = Instant.now(),
            )
    }
}
