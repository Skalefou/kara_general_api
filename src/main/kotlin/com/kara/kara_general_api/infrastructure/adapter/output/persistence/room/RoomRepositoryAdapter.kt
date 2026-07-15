package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomImage
import com.kara.kara_general_api.domain.model.room.RoomCluster
import com.kara.kara_general_api.domain.model.room.RoomImageId
import com.kara.kara_general_api.domain.model.room.vo.BoundingBox
import com.kara.kara_general_api.domain.port.output.RoomRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.util.UUID

@Component
class RoomRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val rowMapper: RoomRowMapper,
) : RoomRepository {

    private val imageRowMapper =
        RowMapper { rs, _ ->
            RoomImage(
                id = RoomImageId(rs.getObject("id", UUID::class.java)),
                objectKey = rs.getString("object_key"),
                position = rs.getInt("position"),
            )
        }

    override fun save(room: Room): Room {
        val sql =
            """
            INSERT INTO rooms (id, name, street, city, postal_code, country, price_per_person_per_hour, currency,
                               latitude, longitude, status, created_at)
            VALUES (:id, :name, :street, :city, :postalCode, :country, :pricePerPersonPerHour, :currency,
                    :latitude, :longitude, :status, :createdAt)
            ON CONFLICT (id) DO UPDATE SET
                name                     = EXCLUDED.name,
                street                   = EXCLUDED.street,
                city                     = EXCLUDED.city,
                postal_code              = EXCLUDED.postal_code,
                country                  = EXCLUDED.country,
                price_per_person_per_hour = EXCLUDED.price_per_person_per_hour,
                currency                 = EXCLUDED.currency,
                latitude                 = EXCLUDED.latitude,
                longitude                = EXCLUDED.longitude,
                status                   = EXCLUDED.status
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", room.id.value)
                .addValue("name", room.name)
                .addValue("street", room.address.street)
                .addValue("city", room.address.city)
                .addValue("postalCode", room.address.postalCode)
                .addValue("country", room.address.country)
                .addValue("pricePerPersonPerHour", room.pricePerPersonPerHour)
                .addValue("currency", room.currency.name)
                .addValue("latitude", room.latitude)
                .addValue("longitude", room.longitude)
                .addValue("status", room.status.name)
                .addValue("createdAt", Timestamp.from(room.createdAt)),
        )
        return room
    }

    override fun findById(id: RoomId): Room? {
        val sql =
            """
            SELECT id, name, street, city, postal_code, country, price_per_person_per_hour, currency,
                   latitude, longitude, status, created_at
            FROM rooms
            WHERE id = :id
            """.trimIndent()
        val room = jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull() ?: return null
        return room.copy(images = findImages(id))
    }

    override fun findAll(page: Int, size: Int): List<Room> {
        val sql =
            """
            SELECT id, name, street, city, postal_code, country, price_per_person_per_hour, currency,
                   latitude, longitude, status, created_at
            FROM rooms
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """.trimIndent()
        val rooms = jdbc.query(sql, mapOf("limit" to size, "offset" to page * size), rowMapper)
        if (rooms.isEmpty()) return rooms
        val imagesByRoom = findImagesByRoomIds(rooms.map { it.id.value })
        return rooms.map { it.copy(images = imagesByRoom[it.id.value].orEmpty()) }
    }

    override fun count(): Long {
        val sql = "SELECT COUNT(*) FROM rooms"
        return jdbc.queryForObject(sql, emptyMap<String, Any>(), Long::class.java) ?: 0
    }

    // Filtre viewport : le BETWEEN sur (latitude, longitude) est servi par l'index idx_rooms_lat_lng.
    // TODO: ne gère pas l'antiméridien (bbox à cheval sur ±180°) — hors scope.
    override fun findInBbox(bbox: BoundingBox, limit: Int): List<Room> {
        val sql =
            """
            SELECT id, name, street, city, postal_code, country, price_per_person_per_hour, currency,
                   latitude, longitude, status, created_at
            FROM rooms
            WHERE latitude BETWEEN :minLat AND :maxLat
              AND longitude BETWEEN :minLng AND :maxLng
            ORDER BY created_at DESC
            LIMIT :limit
            """.trimIndent()
        val params =
            MapSqlParameterSource()
                .addValue("minLat", bbox.minLat)
                .addValue("maxLat", bbox.maxLat)
                .addValue("minLng", bbox.minLng)
                .addValue("maxLng", bbox.maxLng)
                .addValue("limit", limit)
        val rooms = jdbc.query(sql, params, rowMapper)
        if (rooms.isEmpty()) return rooms
        val imagesByRoom = findImagesByRoomIds(rooms.map { it.id.value })
        return rooms.map { it.copy(images = imagesByRoom[it.id.value].orEmpty()) }
    }

    override fun countInBbox(bbox: BoundingBox): Long {
        val sql =
            """
            SELECT COUNT(*)
            FROM rooms
            WHERE latitude BETWEEN :minLat AND :maxLat
              AND longitude BETWEEN :minLng AND :maxLng
            """.trimIndent()
        val params =
            mapOf(
                "minLat" to bbox.minLat,
                "maxLat" to bbox.maxLat,
                "minLng" to bbox.minLng,
                "maxLng" to bbox.maxLng,
            )
        return jdbc.queryForObject(sql, params, Long::class.java) ?: 0
    }

    // Clustering serveur : chaque salle est rangée dans une cellule de grille via FLOOR sur des
    // buckets calculés à partir de lat/lng, puis agrégée (centroïde AVG + COUNT) en SQL — pas de
    // chargement en mémoire. Le WHERE bbox est servi par idx_rooms_lat_lng. NULLIF évite une
    // division par zéro sur une bbox dégénérée (hauteur/largeur nulle) : tout tombe dans un groupe NULL.
    override fun clustersInBbox(bbox: BoundingBox, gridSize: Int): List<RoomCluster> {
        val cellHeight = (bbox.maxLat - bbox.minLat) / gridSize
        val cellWidth = (bbox.maxLng - bbox.minLng) / gridSize
        val sql =
            """
            SELECT AVG(latitude) AS lat, AVG(longitude) AS lng, COUNT(*) AS cnt
            FROM rooms
            WHERE latitude BETWEEN :minLat AND :maxLat
              AND longitude BETWEEN :minLng AND :maxLng
            GROUP BY
                FLOOR((latitude - :minLat) / NULLIF(:cellHeight, 0)),
                FLOOR((longitude - :minLng) / NULLIF(:cellWidth, 0))
            """.trimIndent()
        val params =
            mapOf(
                "minLat" to bbox.minLat,
                "maxLat" to bbox.maxLat,
                "minLng" to bbox.minLng,
                "maxLng" to bbox.maxLng,
                "cellHeight" to cellHeight,
                "cellWidth" to cellWidth,
            )
        return jdbc.query(sql, params) { rs, _ ->
            RoomCluster(
                latitude = rs.getDouble("lat"),
                longitude = rs.getDouble("lng"),
                count = rs.getLong("cnt"),
            )
        }
    }

    override fun deleteById(id: RoomId): Boolean {
        val sql = "DELETE FROM rooms WHERE id = :id"
        val rows = jdbc.update(sql, mapOf("id" to id.value))
        return rows > 0
    }

    override fun addImage(roomId: RoomId, image: RoomImage): RoomImage {
        val sql =
            """
            INSERT INTO room_images (id, room_id, object_key, position, created_at)
            VALUES (:id, :roomId, :objectKey, :position, NOW())
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", image.id.value)
                .addValue("roomId", roomId.value)
                .addValue("objectKey", image.objectKey)
                .addValue("position", image.position),
        )
        return image
    }

    override fun removeImage(roomId: RoomId, imageId: RoomImageId): Boolean {
        val sql = "DELETE FROM room_images WHERE id = :id AND room_id = :roomId"
        val rows =
            jdbc.update(
                sql,
                mapOf("id" to imageId.value, "roomId" to roomId.value),
            )
        return rows > 0
    }

    private fun findImages(roomId: RoomId): List<RoomImage> {
        val sql =
            """
            SELECT id, object_key, position
            FROM room_images
            WHERE room_id = :roomId
            ORDER BY position ASC
            """.trimIndent()
        return jdbc.query(sql, mapOf("roomId" to roomId.value), imageRowMapper)
    }

    private fun findImagesByRoomIds(roomIds: List<UUID>): Map<UUID, List<RoomImage>> {
        val sql =
            """
            SELECT id, room_id, object_key, position
            FROM room_images
            WHERE room_id IN (:roomIds)
            ORDER BY position ASC
            """.trimIndent()
        val rows =
            jdbc.query(sql, mapOf("roomIds" to roomIds)) { rs, _ ->
                rs.getObject("room_id", UUID::class.java) to
                    RoomImage(
                        id = RoomImageId(rs.getObject("id", UUID::class.java)),
                        objectKey = rs.getString("object_key"),
                        position = rs.getInt("position"),
                    )
            }
        return rows.groupBy({ it.first }, { it.second })
    }
}
