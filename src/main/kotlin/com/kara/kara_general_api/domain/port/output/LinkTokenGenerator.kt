package com.kara.kara_general_api.domain.port.output

/** Génère des tokens de lien opaques et imprévisibles (lien global de cagnotte, lien unique de part). */
interface LinkTokenGenerator {
    fun generate(): String
}
