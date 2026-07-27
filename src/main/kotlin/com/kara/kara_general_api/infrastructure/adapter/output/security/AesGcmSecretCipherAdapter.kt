package com.kara.kara_general_api.infrastructure.adapter.output.security

import com.kara.kara_general_api.domain.port.output.SecretCipher
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val KEY_ALGORITHM = "AES"
private const val KEY_LENGTH_BYTES = 32 // AES-256
private const val IV_LENGTH_BYTES = 12 // taille d'IV recommandée pour GCM
private const val TAG_LENGTH_BITS = 128

/**
 * Chiffre les secrets TOTP en AES-256-GCM (JDK, sans dépendance externe). Format de sortie :
 * `base64(IV || ciphertext || tag)` — l'IV aléatoire de 12 octets est préfixé au chiffré, si bien qu'un même
 * secret produit un cryptogramme différent à chaque appel.
 *
 * La clé est fournie par la variable d'environnement `TWO_FACTOR_ENCRYPTION_KEY` (32 octets en base64).
 * L'application refuse de démarrer si elle est absente ou de mauvaise taille. Ni la clé ni le secret
 * déchiffré ne sont jamais écrits dans les logs.
 */
@Component
class AesGcmSecretCipherAdapter(
    @Value("\${TWO_FACTOR_ENCRYPTION_KEY:}") base64Key: String,
) : SecretCipher {
    private val secureRandom = SecureRandom()
    private val key: SecretKeySpec = parseKey(base64Key)

    override fun encrypt(plain: String): String {
        val iv = ByteArray(IV_LENGTH_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    override fun decrypt(cipherText: String): String {
        val raw = Base64.getDecoder().decode(cipherText)
        require(raw.size > IV_LENGTH_BYTES) { "Chiffré A2F invalide : trop court pour contenir un IV." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(TAG_LENGTH_BITS, raw, 0, IV_LENGTH_BYTES),
        )
        val decrypted = cipher.doFinal(raw, IV_LENGTH_BYTES, raw.size - IV_LENGTH_BYTES)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun parseKey(base64Key: String): SecretKeySpec {
        require(base64Key.isNotBlank()) {
            "TWO_FACTOR_ENCRYPTION_KEY est absente : renseignez une clé AES de 32 octets encodée en base64 " +
                "(openssl rand -base64 32)."
        }
        val decoded =
            try {
                Base64.getDecoder().decode(base64Key.trim())
            } catch (cause: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "TWO_FACTOR_ENCRYPTION_KEY n'est pas un base64 valide (attendu : 32 octets encodés).",
                    cause,
                )
            }
        require(decoded.size == KEY_LENGTH_BYTES) {
            "TWO_FACTOR_ENCRYPTION_KEY doit faire exactement $KEY_LENGTH_BYTES octets une fois décodée " +
                "(reçu : ${decoded.size})."
        }
        return SecretKeySpec(decoded, KEY_ALGORITHM)
    }
}
