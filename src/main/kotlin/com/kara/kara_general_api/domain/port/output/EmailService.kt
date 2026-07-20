package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.user.vo.Email
import java.time.Instant

interface EmailService {
    fun sendVerificationCode(email: Email, code: String)

    fun sendAccountDeletionConfirmation(email: Email)

    fun sendPasswordResetCode(email: Email, code: String)

    fun sendServerInvitation(email: Email, firstName: String, temporaryPassword: String, expiresAt: Instant)

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
    fun sendPoolConfirmation(email: Email, roomName: String, startAt: Instant)

    /** Informe un participant que la cagnotte a été annulée (délai échu) : aucun prélèvement effectué. */
    fun sendPoolCancelled(email: Email, participantName: String, roomName: String)
}
