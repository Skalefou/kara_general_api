package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomOption

interface RoomOptionRepository {
    /** Options tarifées rattachées à une salle, ordonnées par libellé. */
    fun findByRoomId(roomId: RoomId): List<RoomOption>
}
