package com.kara.kara_general_api.infrastructure.adapter.output.messaging.imagejob

/**
 * Miroir EXACT du contrat de message `image-results` (worker Rust → API), figé côté
 * `kara-image-worker/CLAUDE.md`. Invariants : [variants] présent SSI `status == "ok"`, [error] présent
 * SSI `status == "failed"`. Le [jobId] est repris tel quel du job d'origine (corrélation).
 *
 * `error.code` reste une chaîne sur le fil (tolérance ascendante à la désérialisation) ; l'énum figée
 * des codes valides est [ImageErrorCode], appliquée côté domaine lors de la persistance.
 */
data class ImageResultMessage(
    val schemaVersion: Int,
    val jobId: String,
    val status: String,
    val variants: List<ImageResultVariant>? = null,
    val error: ImageResultError? = null,
    val processedAt: String? = null,
) {
    fun isOk(): Boolean = status.equals("ok", ignoreCase = true)
}

data class ImageResultVariant(
    val name: String,
    val bucket: String,
    val key: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val contentType: String,
)

data class ImageResultError(
    val code: String,
    val message: String? = null,
)

/**
 * Énum figée des codes d'erreur du worker (SCREAMING_SNAKE_CASE), coordonnée avec `kara-image-worker`.
 * [fromWire] normalise un code reçu et retombe sur [INTERNAL] si le worker envoie un code inconnu, pour
 * ne jamais faire échouer le traitement d'un résultat sur une divergence de contrat.
 */
enum class ImageErrorCode {
    DOWNLOAD_FAILED,
    UNSUPPORTED_FORMAT,
    DECODE_FAILED,
    RESIZE_FAILED,
    UPLOAD_FAILED,
    TIMEOUT,
    INTERNAL,
    ;

    companion object {
        fun fromWire(code: String?): ImageErrorCode = entries.firstOrNull { it.name.equals(code?.trim(), ignoreCase = true) } ?: INTERNAL
    }
}
