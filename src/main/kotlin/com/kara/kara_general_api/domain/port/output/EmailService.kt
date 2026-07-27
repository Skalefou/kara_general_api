package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.vo.Email
import java.time.Instant

interface EmailService {
    fun sendVerificationCode(
        email: Email,
        code: String,
    )

    fun sendAccountDeletionConfirmation(email: Email)

    fun sendPasswordResetCode(
        email: Email,
        code: String,
    )

    /** Notifie l'activation de l'authentification à deux facteurs sur le compte. */
    fun sendTwoFactorEnabled(email: Email)

    /**
     * Notifie la désactivation de l'authentification à deux facteurs — que ce soit à la demande explicite
     * de l'utilisateur ou parce qu'un code de secours a été consommé pour se connecter.
     */
    fun sendTwoFactorDisabled(email: Email)

    fun sendServerInvitation(
        email: Email,
        firstName: String,
        temporaryPassword: String,
        expiresAt: Instant,
    )

    /**
     * Invite un participant à payer sa part de cagnotte via son lien unique. Rappelle que la carte n'est
     * débitée que lorsque toutes les parts ont été payées.
     */
    fun sendPoolInvitation(
        email: Email,
        participantName: String,
        roomName: String,
        shareLinkToken: String,
        deadline: Instant,
    )

    /** Confirme au créateur que la cagnotte est complète : réservation confirmée, tous les paiements capturés. */
    fun sendPoolConfirmation(
        email: Email,
        roomName: String,
        startAt: Instant,
    )

    /** Informe un participant que la cagnotte a été annulée (délai échu) : aucun prélèvement effectué. */
    fun sendPoolCancelled(
        email: Email,
        participantName: String,
        roomName: String,
    )

    /**
     * Confirme au client l'annulation de sa réservation (mode « payer tout »). [refunded] indique si un
     * remboursement a été émis (réservation qui était confirmée) ou non (réservation encore en attente).
     */
    fun sendBookingCancelled(
        email: Email,
        roomName: String,
        startAt: Instant,
        refunded: Boolean,
    )
}
