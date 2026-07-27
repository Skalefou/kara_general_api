package com.kara.kara_general_api.infrastructure.adapter.output.security

import com.kara.kara_general_api.domain.model.user.twofactor.RecoveryCodeNormalizer
import com.kara.kara_general_api.domain.port.output.RecoveryCodeGenerator
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.security.SecureRandom

private const val WORDLIST_PATH = "security/recovery-wordlist-fr.txt"

/** Plancher d'entropie : en deçà, la liste est jugée trop pauvre et l'application refuse de démarrer. */
private const val MIN_WORDLIST_SIZE = 1024

/**
 * Génère des codes de secours composés de mots français tirés au sort par [SecureRandom]. Des mots sont
 * bien plus faciles à recopier à la main (ou à dicter) qu'une suite hexadécimale, pour une entropie
 * équivalente.
 *
 * La wordlist vit dans `resources/security/recovery-wordlist-fr.txt` (un mot par ligne ; lignes vides et
 * commentaires `#` ignorés). Sa taille réelle est documentée dans l'en-tête de ce fichier de ressource.
 */
@Component
class WordlistRecoveryCodeGenerator : RecoveryCodeGenerator {
    private val secureRandom = SecureRandom()
    private val words: List<String> = loadWordlist()

    override fun generate(
        count: Int,
        wordsPerCode: Int,
    ): List<String> {
        require(count > 0) { "Le nombre de codes de secours à générer doit être strictement positif." }
        require(wordsPerCode > 0) { "Un code de secours doit contenir au moins un mot." }
        return List(count) { newCode(wordsPerCode) }
    }

    private fun newCode(wordsPerCode: Int): String =
        RecoveryCodeNormalizer.normalize(
            (1..wordsPerCode).joinToString(" ") { words[secureRandom.nextInt(words.size)] },
        )

    private fun loadWordlist(): List<String> {
        val lines =
            ClassPathResource(WORDLIST_PATH).inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readLines()
            }
        val unique =
            lines
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .distinct()
                .toList()
        check(unique.size >= MIN_WORDLIST_SIZE) {
            "Recovery code wordlist '$WORDLIST_PATH' holds ${unique.size} unique entries, " +
                "at least $MIN_WORDLIST_SIZE are required."
        }
        return unique
    }
}
