package com.kara.kara_general_api.domain.model.user.twofactor

import java.text.Normalizer

private val DIACRITICS = Regex("\\p{M}+")
private val SEPARATORS = Regex("[-_\\s]+")

/**
 * Forme canonique d'un code de secours. **Unique** point de normalisation du projet : appliquée aussi bien
 * à la génération (avant hachage) qu'à la vérification (avant comparaison), afin que la saisie de
 * l'utilisateur — casse, accents ajoutés par un clavier prédictif, tirets, espaces multiples — n'invalide
 * jamais un code correct.
 *
 * Étapes : trim → minuscules → décomposition NFD et suppression des diacritiques → tirets/underscores
 * remplacés par des espaces → espaces multiples réduits à un seul.
 */
object RecoveryCodeNormalizer {
    fun normalize(raw: String): String {
        val lowercased = raw.trim().lowercase()
        val decomposed = Normalizer.normalize(lowercased, Normalizer.Form.NFD)
        val withoutDiacritics = DIACRITICS.replace(decomposed, "")
        return SEPARATORS.replace(withoutDiacritics, " ").trim()
    }
}
