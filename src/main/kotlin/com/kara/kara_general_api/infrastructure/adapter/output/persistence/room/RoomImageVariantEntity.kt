package com.kara.kara_general_api.infrastructure.adapter.output.persistence.room

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

/**
 * Variante d'image de salle produite par le worker (clé dans le bucket public). Rôle unique : génération du
 * DDL en dev. La FK `image_id` cible `room_images.image_id` (ON DELETE CASCADE côté init.sql prod).
 */
@Entity
@Table(name = "room_image_variants")
class RoomImageVariantEntity(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "image_id", nullable = false, columnDefinition = "uuid")
    var imageId: UUID,
    @Column(nullable = false, columnDefinition = "varchar(50)")
    var name: String,
    @Column(name = "object_key", nullable = false, columnDefinition = "varchar(512)")
    var objectKey: String,
    @Column(nullable = false, columnDefinition = "int")
    var width: Int,
    @Column(nullable = false, columnDefinition = "int")
    var height: Int,
    @Column(name = "size_bytes", nullable = false, columnDefinition = "bigint")
    var sizeBytes: Long,
    @Column(name = "content_type", nullable = false, columnDefinition = "varchar(100)")
    var contentType: String,
)
