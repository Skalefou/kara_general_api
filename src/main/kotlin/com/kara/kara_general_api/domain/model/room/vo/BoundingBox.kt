package com.kara.kara_general_api.domain.model.room.vo

/**
 * Fenêtre géographique (viewport) délimitée par ses coins sud-ouest et nord-est.
 *
 * Les bornes de latitude/longitude sont validées en réutilisant [Coordinates].
 *
 * TODO: l'antiméridien (bbox à cheval sur ±180°, où minLng > maxLng) n'est pas géré — hors scope.
 */
data class BoundingBox(
    val minLat: Double,
    val minLng: Double,
    val maxLat: Double,
    val maxLng: Double,
) {
    init {
        Coordinates(minLat, minLng)
        Coordinates(maxLat, maxLng)
        require(minLat <= maxLat) { "minLat doit être inférieure ou égale à maxLat" }
        require(minLng <= maxLng) { "minLng doit être inférieure ou égale à maxLng" }
    }
}
