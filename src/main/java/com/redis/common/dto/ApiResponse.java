// ─────────────────────────────────────────────────────────────
// ApiResponse.java — Generic wrapper for all API responses
// ─────────────────────────────────────────────────────────────
package com.redis.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter                                          // Only Getter provided for immutable response
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)       // Null fields are omitted from JSON serialization
public class ApiResponse<T> {

    // ─── Status ────────────────────────────────────────────────────────────────
    private final boolean success;

    // ─── Message ───────────────────────────────────────────────────────────────
    private final String message;

    // Machine-readable error code for client-side programmatic handling (null on success)
    private final String errorCode;

    // ─── Payload ───────────────────────────────────────────────────────────────
    private final T data;

    // ─── FIX #4: Validation errors list — @Valid fail hone pe field-wise detail
    private final List<ValidationError> errors;

    // ─── Meta ──────────────────────────────────────────────────────────────────
    private final Boolean fromCache;             // FIX #7: NON_NULL handle karega

    // ─── FIX #2 & #3: LocalDateTime + @JsonFormat — consistent format ──────────
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    // ═══════════════════════════════════════════════════════════════════════════
    //  SUCCESS FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Standard success response returning a data payload */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** Cache-aware success response indicating if resource was served from cache */
    public static <T> ApiResponse<T> success(String message, T data, boolean fromCache) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .fromCache(fromCache)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** Empty success response for confirmations (e.g. updates/deletions) */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ERROR FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Standard error response with a human-readable message */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** Error response containing a machine-readable error code */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** Error response containing errorCode and payload data */
    public static <T> ApiResponse<T> error(String message, String errorCode, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** Validation error response returning field-specific validation failures */
    public static <T> ApiResponse<T> validationError(
            String message,
            List<ValidationError> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode("VALIDATION_FAILED")
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  NESTED — Validation Error Detail
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Validation error detail mapping a specific field to its invalid value and error message.
     *
     * Example JSON output:
     * {
     *   "field": "price",
     *   "rejectedValue": -5.00,
     *   "message": "Price must be at least 0.01"
     * }
     */
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationError {
        private final String field;
        private final Object rejectedValue;
        private final String message;
    }
}