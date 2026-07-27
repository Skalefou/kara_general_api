package com.kara.kara_general_api.domain.port.output

/**
 * Génère des codes de secours composés de mots (plus faciles à recopier à la main qu'une suite
 * hexadécimale). Les codes retournés sont **en clair** : ils ne sont affichés qu'une seule fois, puis
 * seuls leurs hachés sont conservés.
 */
interface RecoveryCodeGenerator {
    fun generate(
        count: Int,
        wordsPerCode: Int,
    ): List<String>
}
