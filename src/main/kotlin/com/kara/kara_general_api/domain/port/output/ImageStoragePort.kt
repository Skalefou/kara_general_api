package com.kara.kara_general_api.domain.port.output

import java.time.Duration

/** Visibilité de stockage : PUBLIC = bucket public servi par le CDN, PRIVATE = bucket privé (URL signée). */
enum class ImageVisibility {
    PUBLIC,
    PRIVATE,
}

/**
 * Port secondaire de stockage d'images (implémenté par un adaptateur GCS).
 * Le domaine manipule des clés d'objet opaques ; la construction des URL reste dans l'adaptateur.
 */
interface ImageStoragePort {
    /** Téléverse [bytes] sous la clé [key] dans le bucket correspondant à [visibility]. */
    fun upload(visibility: ImageVisibility, key: String, bytes: ByteArray, contentType: String)

    /** Supprime l'objet [key] du bucket correspondant à [visibility] (no-op si absent). */
    fun delete(visibility: ImageVisibility, key: String)

    /** URL signée V4 (lecture) valable [ttl] pour un objet du bucket privé. */
    fun signedUrl(key: String, ttl: Duration): String

    /** URL publique (CDN) d'un objet du bucket public. */
    fun publicUrl(key: String): String
}
