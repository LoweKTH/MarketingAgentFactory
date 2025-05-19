package com.exjobb.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Generic DTO for standardized API responses.
 *
 * This class provides a consistent structure for all API responses,
 * whether successful or error responses. It helps maintain a uniform
 * interface between the backend and frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Only include non-null fields in JSON
public class ApiResponse<T> {

    /**
     * Indicates whether the operation was successful.
     * True for successful operations, false for errors.
     */
    private Boolean success;

    /**
     * Human-readable message about the operation result.
     * Provides context for both success and error cases.
     */
    private String message;

    /**
     * The actual data payload of the response.
     * Generic type allows for flexibility in response data structure.
     */
    private T data;

    /**
     * Error code for failed operations.
     * Provides programmatic error identification for the frontend.
     */
    private String errorCode;

    /**
     * Detailed error information for debugging.
     * May include stack traces or validation errors in development.
     */
    private Object errorDetails;

    /**
     * Timestamp when the response was generated.
     * Useful for debugging and request tracing.
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Request identifier for tracing.
     * Helps correlate requests with responses in logs.
     */
    private String requestId;

    /**
     * API version that processed this request.
     * Useful for API versioning and compatibility.
     */
    private String apiVersion;

    // ================================================
    // Convenience factory methods for common responses
    // ================================================

    /**
     * Create a successful response with data.
     *
     * @param data The response data
     * @param message Success message
     * @param <T> Type of the response data
     * @return Successful ApiResponse
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a successful response with just a message.
     *
     * @param message Success message
     * @param <T> Type parameter for consistency
     * @return Successful ApiResponse
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an error response with message and error code.
     *
     * @param message Error message
     * @param errorCode Error code for programmatic handling
     * @param <T> Type parameter for consistency
     * @return Error ApiResponse
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create an error response with message, error code, and details.
     *
     * @param message Error message
     * @param errorCode Error code for programmatic handling
     * @param errorDetails Detailed error information
     * @param <T> Type parameter for consistency
     * @return Error ApiResponse
     */
    public static <T> ApiResponse<T> error(String message, String errorCode, Object errorDetails) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errorDetails(errorDetails)
                .timestamp(LocalDateTime.now())
                .build();
    }
}