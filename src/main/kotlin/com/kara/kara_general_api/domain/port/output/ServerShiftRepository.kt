package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.servershift.ServerShift
import com.kara.kara_general_api.domain.model.servershift.ServerShiftId
import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant

interface ServerShiftRepository {
    /** Persiste (upsert) le créneau d'agenda. */
    fun save(shift: ServerShift): ServerShift

    fun findById(id: ServerShiftId): ServerShift?

    /**
     * Liste les créneaux, filtrables par serveur et/ou salle et par fenêtre temporelle. Un filtre null
     * n'applique aucune restriction sur ce critère. Ordonné par [ServerShift.startAt] croissant.
     */
    fun findAll(serverId: UserId?, roomId: RoomId?, from: Instant?, to: Instant?): List<ServerShift>

    /**
     * Vrai s'il existe déjà un créneau du même serveur qui chevauche [startAt, endAt), en excluant
     * éventuellement le créneau [excludeId] (utile lors d'une mise à jour).
     */
    fun existsOverlappingForServer(
        serverId: UserId,
        startAt: Instant,
        endAt: Instant,
        excludeId: ServerShiftId?,
    ): Boolean

    /** Supprime le créneau et retourne vrai si une ligne a été supprimée. */
    fun deleteById(id: ServerShiftId): Boolean

    /**
     * Identifiants des serveurs rattachés à un créneau [startAt, endAt) dans la salle [roomId] : ceux dont
     * un créneau d'agenda chevauche cet intervalle. Sert à déterminer les serveurs d'une réservation.
     */
    fun findServerIdsAssignedTo(roomId: RoomId, startAt: Instant, endAt: Instant): Set<UserId>
}
