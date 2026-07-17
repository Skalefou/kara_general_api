package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.service.ServiceId

/**
 * Gestion de la liaison salle↔service (table `room_services`). Ce port ne porte que le rattachement
 * d'un service du catalogue global à une salle ; le prix/label/description vivent sur `services`.
 */
interface RoomServiceRepository {
    /** Crée les liaisons de la salle vers les services fournis (les doublons sont ignorés). */
    fun addLinks(roomId: RoomId, serviceIds: List<ServiceId>)

    /**
     * Remplace l'ensemble des liaisons de la salle par celles fournies : supprime toutes les
     * liaisons existantes de la salle, puis crée celles de [serviceIds] (liste vide = tout détacher).
     */
    fun replaceLinks(roomId: RoomId, serviceIds: List<ServiceId>)

    /** Supprime toutes les liaisons de la salle. Retourne le nombre de liaisons supprimées. */
    fun deleteByRoomId(roomId: RoomId): Int

    /** Identifiants des services actuellement attachés à la salle. */
    fun findServiceIdsByRoomId(roomId: RoomId): List<ServiceId>
}
