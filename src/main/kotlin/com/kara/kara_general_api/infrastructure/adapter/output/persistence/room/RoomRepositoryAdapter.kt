package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import com.kara.kara_general_api.domain.model.room.Room
import com.kara.kara_general_api.domain.model.room.RoomCluster
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.room.RoomImage
import com.kara.kara_general_api.domain.model.room.RoomImageId
import com.kara.kara_general_api.domain.model.room.RoomImageStatus
import com.kara.kara_general_api.domain.model.room.RoomImageVariant
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
    private val variantRowMapper =
        RowMapper { rs, _ ->
            rs.getObject("image_id", UUID::class.java) to
                RoomImageVariant(
                    name = rs.getString("name"),
                    objectKey = rs.getString("object_key"),
                    width = rs.getInt("width"),
                    height = rs.getInt("height"),
                    sizeBytes = rs.getLong("size_bytes"),
                    contentType = rs.getString("content_type"),
                )
        }

    override fun save(room: Room): Room {
        val sql =
            """
            INSERT INTO rooms (id, name, description, street, city, postal_code, country, price_per_person_per_hour,
                               currency, max_capacity, is_there_wifi, is_there_sono_pro, is_there_air_conditioning,
                               latitude, longitude, status, opens_at, closes_at, time_zone, created_at)
            VALUES (:id, :name, :description, :street, :city, :postalCode, :country, :pricePerPersonPerHour,
                    :currency, :maxCapacity, :isThereWifi, :isThereSonoPro, :isThereAirConditioning,
                    :latitude, :longitude, :status, :opensAt, :closesAt, :timeZone, :createdAt)
            ON CONFLICT (id) DO UPDATE SET
                name                      = EXCLUDED.name,
                description               = EXCLUDED.description,
                street                    = EXCLUDED.street,
                city                      = EXCLUDED.city,
                postal_code               = EXCLUDED.postal_code,
                country                   = EXCLUDED.country,
                price_per_person_per_hour = EXCLUDED.price_per_person_per_hour,
                currency                  = EXCLUDED.currency,
                max_capacity              = EXCLUDED.max_capacity,
                is_there_wifi             = EXCLUDED.is_there_wifi,
                is_there_sono_pro         = EXCLUDED.is_there_sono_pro,
                is_there_air_conditioning = EXCLUDED.is_there_air_conditioning,
                latitude                  = EXCLUDED.latitude,
                longitude                 = EXCLUDED.longitude,
                status                    = EXCLUDED.status,
                opens_at                  = EXCLUDED.opens_at,
                closes_at                 = EXCLUDED.closes_at,
                time_zone                 = EXCLUDED.time_zone
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", room.id.value)
                .addValue("name", room.name)
                .addValue("description", room.description)
                .addValue("street", room.address.street)
                .addValue("city", room.address.city)
                .addValue("postalCode", room.address.postalCode)
                .addValue("country", room.address.country)
                .addValue("pricePerPersonPerHour", room.pricePerPersonPerHour)
                .addValue("currency", room.currency.name)
                .addValue("maxCapacity", room.maxCapacity)
                .addValue("isThereWifi", room.isThereWifi)
                .addValue("isThereSonoPro", room.isThereSonoPro)
                .addValue("isThereAirConditioning", room.isThereAirConditioning)
                .addValue("latitude", room.latitude)
                .addValue("longitude", room.longitude)
                .addValue("status", room.status.name)
                .addValue("opensAt", room.opensAt)
                .addValue("closesAt", room.closesAt)
                .addValue("timeZone", room.timeZone.id)
                .addValue("createdAt", Timestamp.from(room.createdAt)),
        )
        return room
    }

    override fun findById(id: RoomId): Room? {
        val sql =
            """
            SELECT id, name, description, street, city, postal_code, country, price_per_person_per_hour, currency,
                   max_capacity, is_there_wifi, is_there_sono_pro, is_there_air_conditioning,
                   latitude, longitude, status, opens_at, closes_at, time_zone, created_at
            FROM rooms
            WHERE id = :id
            """.trimIndent()
        val room = jdbc.query(sql, mapOf("id" to id.value), rowMapper).firstOrNull() ?: return null
        return room.copy(images = findImages(id))
    }

    override fun findAll(
        page: Int,
        size: Int,
    ): List<Room> {
        val sql =
            """
            SELECT id, name, description, street, city, postal_code, country, price_per_person_per_hour, currency,
                   max_capacity, is_there_wifi, is_there_sono_pro, is_there_air_conditioning,
                   latitude, longitude, status, opens_at, closes_at, time_zone, created_at
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
    override fun findInBbox(
        bbox: BoundingBox,
        limit: Int,
    ): List<Room> {
        val sql =
            """
            SELECT id, name, description, street, city, postal_code, country, price_per_person_per_hour, currency,
                   max_capacity, is_there_wifi, is_there_sono_pro, is_there_air_conditioning,
                   latitude, longitude, status, opens_at, closes_at, time_zone, created_at
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
    override fun clustersInBbox(
        bbox: BoundingBox,
        gridSize: Int,
    ): List<RoomCluster> {
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

    override fun addImage(
        roomId: RoomId,
        image: RoomImage,
    ): RoomImage {
        // image_id = id : identité unique de l'image, reprise dans les clés de variantes et corrélée au worker.
        val sql =
            """
            INSERT INTO room_images (id, room_id, image_id, object_key, status, error_code, position, created_at)
            VALUES (:id, :roomId, :imageId, :objectKey, :status, :errorCode, :position, NOW())
            """.trimIndent()
        jdbc.update(
            sql,
            MapSqlParameterSource()
                .addValue("id", image.id.value)
                .addValue("roomId", roomId.value)
                .addValue("imageId", image.id.value)
                .addValue("objectKey", image.objectKey)
                .addValue("status", image.status.name)
                .addValue("errorCode", image.errorCode)
                .addValue("position", image.position),
        )
        return image
    }

    override fun removeImage(
        roomId: RoomId,
        imageId: RoomImageId,
    ): Boolean {
        // Les variantes sont supprimées par la FK ON DELETE CASCADE (image_id).
        val sql = "DELETE FROM room_images WHERE id = :id AND room_id = :roomId"
        val rows =
            jdbc.update(
                sql,
                mapOf("id" to imageId.value, "roomId" to roomId.value),
            )
        return rows > 0
    }

    override fun markImageReady(
        imageId: UUID,
        variants: List<RoomImageVariant>,
    ) {
        val updated =
            jdbc.update(
                "UPDATE room_images SET status = 'READY', error_code = NULL WHERE image_id = :imageId",
                mapOf("imageId" to imageId),
            )
        if (updated == 0) return // image supprimée entre-temps : rien à faire (idempotent)
        // Rejeu at-least-once : on réécrit l'ensemble des variantes (clés déterministes → même contenu).
        jdbc.update("DELETE FROM room_image_variants WHERE image_id = :imageId", mapOf("imageId" to imageId))
        val sql =
            """
            INSERT INTO room_image_variants (id, image_id, name, object_key, width, height, size_bytes, content_type)
            VALUES (:id, :imageId, :name, :objectKey, :width, :height, :sizeBytes, :contentType)
            """.trimIndent()
        variants.forEach { variant ->
            jdbc.update(
                sql,
                MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("imageId", imageId)
                    .addValue("name", variant.name)
                    .addValue("objectKey", variant.objectKey)
                    .addValue("width", variant.width)
                    .addValue("height", variant.height)
                    .addValue("sizeBytes", variant.sizeBytes)
                    .addValue("contentType", variant.contentType),
            )
        }
    }

    override fun markImageFailed(
        imageId: UUID,
        errorCode: String,
    ) {
        jdbc.update(
            "UPDATE room_images SET status = 'FAILED', error_code = :errorCode WHERE image_id = :imageId",
            mapOf("imageId" to imageId, "errorCode" to errorCode),
        )
    }

    private fun findImages(roomId: RoomId): List<RoomImage> {
        val sql =
            """
            SELECT id, image_id, object_key, position, status, error_code
            FROM room_images
            WHERE room_id = :roomId
            ORDER BY position ASC
            """.trimIndent()
        val images =
            jdbc.query(sql, mapOf("roomId" to roomId.value)) { rs, _ -> mapImageRow(rs) }
        return attachVariants(images)
    }

    private fun findImagesByRoomIds(roomIds: List<UUID>): Map<UUID, List<RoomImage>> {
        val sql =
            """
            SELECT id, room_id, image_id, object_key, position, status, error_code
            FROM room_images
            WHERE room_id IN (:roomIds)
            ORDER BY position ASC
            """.trimIndent()
        val rows =
            jdbc.query(sql, mapOf("roomIds" to roomIds)) { rs, _ ->
                rs.getObject("room_id", UUID::class.java) to mapImageRow(rs)
            }
        val variantsByImage = loadVariants(rows.map { it.second.id.value })
        return rows
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, images) -> images.map { it.copy(variants = variantsByImage[it.id.value].orEmpty()) } }
    }

    private fun mapImageRow(rs: java.sql.ResultSet): RoomImage =
        RoomImage(
            id = RoomImageId(rs.getObject("image_id", UUID::class.java)),
            objectKey = rs.getString("object_key"),
            position = rs.getInt("position"),
            status = RoomImageStatus.valueOf(rs.getString("status")),
            errorCode = rs.getString("error_code"),
        )

    private fun attachVariants(images: List<RoomImage>): List<RoomImage> {
        if (images.isEmpty()) return images
        val variantsByImage = loadVariants(images.map { it.id.value })
        return images.map { it.copy(variants = variantsByImage[it.id.value].orEmpty()) }
    }

    private fun loadVariants(imageIds: List<UUID>): Map<UUID, List<RoomImageVariant>> {
        if (imageIds.isEmpty()) return emptyMap()
        val sql =
            """
            SELECT image_id, name, object_key, width, height, size_bytes, content_type
            FROM room_image_variants
            WHERE image_id IN (:imageIds)
            ORDER BY name ASC
            """.trimIndent()
        return jdbc
            .query(sql, mapOf("imageIds" to imageIds), variantRowMapper)
            .groupBy({ it.first }, { it.second })
    }
}
