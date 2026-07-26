package com.kara.kara_general_api.domain.port.output

import com.kara.kara_general_api.domain.model.room.vo.Address
import com.kara.kara_general_api.domain.model.room.vo.Coordinates

/**
 * Port secondaire de géocodage : convertit une adresse en coordonnées géographiques.
 */
interface GeocodingPort {
    /**
     * @return les coordonnées de l'adresse, ou null si l'adresse n'est pas résolue (aucun résultat).
     * @throws GeocodingException si le service de géocodage est indisponible (erreur technique).
     */
    fun geocode(address: Address): Coordinates?
}

/** Erreur technique : le service de géocodage est injoignable ou a répondu en erreur. */
class GeocodingException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
