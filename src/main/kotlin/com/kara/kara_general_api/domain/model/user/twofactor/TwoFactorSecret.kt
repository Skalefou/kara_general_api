package com.kara.kara_general_api.domain.model.user.twofactor

import com.kara.kara_general_api.domain.model.user.UserId
import java.time.Instant

/**
 * Secret TOTP (RFC 6238) rattaché à un compte. Un compte porte au plus un secret.
 *
 * [secretCipher] est **toujours** la forme chiffrée du secret : le secret en clair ne vit qu'en mémoire,
 * le temps d'afficher le QR code ou de vérifier un code. Il n'est ni persisté ni logué en clair.
 *
 * [lastUsedStep] porte l'anti-rejeu : le dernier pas de temps TOTP (fenêtre de 30 s) déjà consommé. Un code
 * appartenant à un pas déjà consommé est refusé, ce qui empêche de rejouer un code intercepté.
 */
data class TwoFactorSecret(
    val userId: UserId,
    val secretCipher: String,
    val status: TwoFactorStatus,
    val createdAt: Instant,
    val activatedAt: Instant? = null,
    val lastUsedStep: Long? = null,
) {
    /** L'A2F n'est exigée à la connexion que lorsque l'activation a été confirmée par un premier code valide. */
    val isActive: Boolean get() = status == TwoFactorStatus.ACTIVE

    /** Confirme l'activation : le pas [step] du code qui vient d'être validé est immédiatement consommé. */
    fun activate(
        now: Instant,
        step: Long,
    ): TwoFactorSecret =
        copy(
            status = TwoFactorStatus.ACTIVE,
            activatedAt = now,
            lastUsedStep = step,
        )

    /** Consomme le pas de temps [step] (anti-rejeu). */
    fun withLastUsedStep(step: Long): TwoFactorSecret = copy(lastUsedStep = step)

    /** Vrai si [step] a déjà servi : le code correspondant doit être refusé. */
    fun isStepAlreadyUsed(step: Long): Boolean = lastUsedStep != null && lastUsedStep >= step

    companion object {
        /** Crée un secret en attente de confirmation (le QR code vient d'être affiché). */
        fun pending(
            userId: UserId,
            secretCipher: String,
            now: Instant,
        ): TwoFactorSecret =
            TwoFactorSecret(
                userId = userId,
                secretCipher = secretCipher,
                status = TwoFactorStatus.PENDING,
                createdAt = now,
            )
    }
}
