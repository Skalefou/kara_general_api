package com.kara.kara_general_api.domain.port.output

import java.time.Duration

/** Visibilité de stockage : PUBLIC = bucket public servi par le CDN, PRIVATE = bucket privé (URL signée). */
enum class ImageVisibility {
    PUBLIC,
    PRIVATE,
}

/**
 * Port secondaire de stockage d'objets (implémenté par un adaptateur GCS). Historiquement dédié aux images,
 * il sert de **magasin d'objets générique** : le domaine manipule des clés opaques et un [contentType]
 * arbitraire (images publiques via CDN, mais aussi documents privés comme les PDF de reçus servis par URL
 * signée). La construction des URL et le choix du bucket restent dans l'adaptateur.
 */
interface ImageStoragePort {
    /** Téléverse (écrase si présent) [bytes] sous la clé [key] dans le bucket correspondant à [visibility]. */
    fun upload(
        visibility: ImageVisibility,
        key: String,
        bytes: ByteArray,
        contentType: String,
    )

    /** Vrai si l'objet [key] existe déjà dans le bucket correspondant à [visibility]. */
    fun exists(
        visibility: ImageVisibility,
        key: String,
    ): Boolean

    /** Supprime l'objet [key] du bucket correspondant à [visibility] (no-op si absent). */
    fun delete(
        visibility: ImageVisibility,
        key: String,
    )

    /** URL signée V4 (lecture) valable [ttl] pour un objet du bucket privé. */
    fun signedUrl(
        key: String,
        ttl: Duration,
    ): String

    /** URL publique (CDN) d'un objet du bucket public. */
    fun publicUrl(key: String): String
}
