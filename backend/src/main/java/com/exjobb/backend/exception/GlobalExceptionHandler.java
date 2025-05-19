package com.exjobb.backend.exception;

import com.exjobb.backend.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Marketing Agent Factory backend.
 *
 * This class provides centralized exception handling across the entire application.
 * It catches exceptions thrown by controllers and services, logs them appropriately,
 * and returns consistent error responses to the frontend.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle task processing exceptions.
     */
    @ExceptionHandler(TaskProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handleTaskProcessingException(
            TaskProcessingException ex, WebRequest request) {

        log.error("Task processing failed: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                "TASK_PROCESSING_ERROR",
                this.createErrorDetails(ex, request)
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle validation exceptions from request body validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        log.warn("Validation failed for request: {}", ex.getMessage());

        // Extract field validation errors
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                fieldErrors.put("global", error.getDefaultMessage());
            }
        });

        ApiResponse<Void> response = ApiResponse.error(
                "Validation failed",
                "VALIDATION_ERROR",
                fieldErrors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle constraint violation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        log.warn("Constraint violation: {}", ex.getMessage());

        // Extract constraint violation details
        Map<String, String> violations = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        ApiResponse<Void> response = ApiResponse.error(
                "Validation constraints violated",
                "CONSTRAINT_VIOLATION",
                violations
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle binding exceptions.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(
            BindException ex, WebRequest request) {

        log.warn("Binding failed: {}", ex.getMessage());

        Map<String, String> bindingErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                bindingErrors.put(error.getField(), error.getDefaultMessage())
        );

        ApiResponse<Void> response = ApiResponse.error(
                "Request binding failed",
                "BINDING_ERROR",
                bindingErrors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        log.warn("Authentication failed: {}", ex.getMessage());

        String message = "Authentication failed";
        String errorCode = "AUTHENTICATION_ERROR";

        // Provide more specific messages for common cases
        if (ex instanceof BadCredentialsException) {
            message = "Invalid username or password";
            errorCode = "BAD_CREDENTIALS";
        }

        ApiResponse<Void> response = ApiResponse.error(message, errorCode);

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {

        log.warn("Access denied: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                "Access denied - insufficient permissions",
                "ACCESS_DENIED"
        );

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                "ILLEGAL_ARGUMENT",
                this.createErrorDetails(ex, request)
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle WebClient response exceptions.
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleWebClientResponseException(
            WebClientResponseException ex, WebRequest request) {

        log.error("Python service communication failed: status={}, body={}",
                ex.getStatusCode(), ex.getResponseBodyAsString());

        String message = "Content generation service temporarily unavailable";
        String errorCode = "SERVICE_COMMUNICATION_ERROR";

        // Provide more specific error messages based on status code
        if (ex.getStatusCode().is4xxClientError()) {
            message = "Invalid request to content generation service";
            errorCode = "SERVICE_CLIENT_ERROR";
        } else if (ex.getStatusCode().is5xxServerError()) {
            message = "Content generation service is experiencing issues";
            errorCode = "SERVICE_SERVER_ERROR";
        }

        ApiResponse<Void> response = ApiResponse.error(
                message,
                errorCode,
                Map.of(
                        "serviceStatus", ex.getStatusCode().value(),
                        "serviceResponse", ex.getResponseBodyAsString()
                )
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_GATEWAY);
    }

    /**
     * Handle runtime exceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        log.error("Unexpected runtime error: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error(
                "An unexpected error occurred",
                "RUNTIME_ERROR",
                this.createErrorDetails(ex, request)
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(
            Exception ex, WebRequest request) {

        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error(
                "An internal server error occurred",
                "INTERNAL_SERVER_ERROR",
                this.createErrorDetails(ex, request)
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Create error details for debugging purposes.
     *
     * This method creates a standardized error details object
     * that includes useful information for debugging while
     * being careful not to expose sensitive information.
     *
     * @param ex The exception
     * @param request The web request
     * @return Map containing error details
     */
    private Map<String, Object> createErrorDetails(Exception ex, WebRequest request) {
        Map<String, Object> details = new HashMap<>();

        // Add basic error information
        details.put("timestamp", LocalDateTime.now().toString());
        details.put("path", request.getDescription(false));

        // Add exception class name (useful for debugging)
        details.put("exceptionType", ex.getClass().getSimpleName());

        // In development, include stack trace; in production, exclude it
        String env = System.getProperty("spring.profiles.active", "dev");
        if ("dev".equals(env) || "development".equals(env)) {
            // Include stack trace in development
            details.put("stackTrace", this.getStackTraceAsString(ex));
        }

        return details;
    }

    /**
     * Convert stack trace to string for error details.
     *
     * @param ex The exception
     * @return Stack trace as string
     */
    private String getStackTraceAsString(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }
}