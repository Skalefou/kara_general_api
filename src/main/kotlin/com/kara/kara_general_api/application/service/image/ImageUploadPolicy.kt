package com.kara.kara_general_api.application.service.image

/** Politique de validation partagée pour tout téléversement d'image (profil et salle). */
object ImageUploadPolicy {
    val ALLOWED_CONTENT_TYPES =
        setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/avif",
            "image/heic",
            "image/heif",
        )
    const val MAX_SIZE_BYTES = 5L * 1024 * 1024

    fun isAllowedType(contentType: String?): Boolean = contentType in ALLOWED_CONTENT_TYPES

    fun isWithinSize(size: Int): Boolean = size <= MAX_SIZE_BYTES

    /** Extension de fichier canonique pour un type MIME autorisé. */
    fun extensionFor(contentType: String): String =
        when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/avif" -> "avif"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            else -> "bin"
        }
}
