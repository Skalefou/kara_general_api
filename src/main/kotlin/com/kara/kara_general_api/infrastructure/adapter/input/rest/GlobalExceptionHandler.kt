package com.kara.kara_general_api.infrastructure.adapter.input.rest

import com.kara.kara_general_api.domain.port.output.GeocodingException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(GeocodingException::class)
    fun handleGeocoding(ex: GeocodingException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_GATEWAY,
            "Le service de géolocalisation est momentanément indisponible. Réessayez plus tard.",
        ).apply {
            title = "Service de géolocalisation indisponible"
            setProperty("code", "GEOCODING_UNAVAILABLE")
        }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSize(ex: MaxUploadSizeExceededException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "L'image dépasse la taille maximale autorisée (5 Mo).",
        ).apply {
            title = "Image trop volumineuse"
            setProperty("code", "IMAGE_TOO_LARGE")
        }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Requête invalide").apply {
            title = "Requête invalide"
            setProperty("code", "INVALID_REQUEST")
        }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail {
        val detail = ex.bindingResult.fieldErrors.joinToString(" ") { "${it.field}: ${it.defaultMessage}" }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail).apply {
            title = "Requête invalide"
            setProperty("code", "VALIDATION_ERROR")
        }
    }
}
