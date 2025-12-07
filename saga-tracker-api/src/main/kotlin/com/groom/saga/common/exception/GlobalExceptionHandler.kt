package com.groom.saga.common.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(SagaNotFoundException::class)
    fun handleSagaNotFoundException(ex: SagaNotFoundException): ResponseEntity<ErrorResponse> {
        logger.warn { "Saga not found: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    error = "NOT_FOUND",
                    message = ex.message ?: "Saga not found",
                    timestamp = Instant.now()
                )
            )
    }

    @ExceptionHandler(DuplicateEventException::class)
    fun handleDuplicateEventException(ex: DuplicateEventException): ResponseEntity<ErrorResponse> {
        logger.warn { "Duplicate event: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    error = "CONFLICT",
                    message = ex.message ?: "Duplicate event",
                    timestamp = Instant.now()
                )
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn { "Bad request: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    error = "BAD_REQUEST",
                    message = ex.message ?: "Invalid request",
                    timestamp = Instant.now()
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error(ex) { "Internal server error: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    error = "INTERNAL_ERROR",
                    message = "An unexpected error occurred",
                    timestamp = Instant.now()
                )
            )
    }
}

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Instant,
    val details: Map<String, Any>? = null
)
