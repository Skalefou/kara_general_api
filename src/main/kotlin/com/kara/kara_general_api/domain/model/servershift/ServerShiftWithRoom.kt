package com.kara.kara_general_api.domain.model.servershift

/**
 * Vue enrichie d'un créneau pour l'affichage côté serveur : le créneau brut plus le nom et la ville de
 * la salle où le serveur doit se rendre (évite un aller-retour de résolution côté client).
 */
data class ServerShiftWithRoom(
    val shift: ServerShift,
    val roomName: String,
    val roomCity: String,
)
