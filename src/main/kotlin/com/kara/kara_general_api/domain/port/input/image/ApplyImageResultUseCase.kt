package com.kara.kara_general_api.domain.port.input.image

import java.util.UUID

/** Une variante retournée par le worker, projetée en modèle métier (sans dépendance au format de message). */
data class AppliedImageVariant(
    val name: String,
    val objectKey: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val contentType: String,
)

/**
 * Résultat d'un traitement d'image à appliquer en base. [success] discrimine ok/échec ; [variants] n'est
 * renseigné qu'en cas de succès, [errorCode] (SCREAMING_SNAKE_CASE) qu'en cas d'échec.
 */
data class ApplyImageResultCommand(
    val jobId: UUID,
    val success: Boolean,
    val variants: List<AppliedImageVariant> = emptyList(),
    val errorCode: String? = null,
)

/**
 * Port primaire : applique idempotemment (livraison at-least-once, rejeu = écrasement) le résultat d'un job
 * d'image sur l'entité corrélée (salle → variantes + statut ; profil → clés de variantes + statut).
 */
interface ApplyImageResultUseCase {
    fun apply(command: ApplyImageResultCommand)
}
