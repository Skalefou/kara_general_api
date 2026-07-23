package com.kara.kara_general_api.domain.model.user

/** Cycle de vie de la photo de profil, traitée de façon asynchrone par le worker (variantes privées). */
enum class PhotoStatus {
    /** Original téléversé, variantes (thumbnail/full) en cours de génération. */
    PROCESSING,

    /** Variantes générées : [User.photoThumbnailKey] et [User.photoFullKey] sont renseignées. */
    READY,

    /** Le worker a échoué à traiter la photo. */
    FAILED,
}
