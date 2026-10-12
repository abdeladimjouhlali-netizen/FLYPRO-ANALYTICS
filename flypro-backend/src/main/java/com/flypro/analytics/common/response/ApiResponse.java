package com.flypro.analytics.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Enveloppe de succès API standardisée.
 *
 * @param <T> type du payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, message, null);
    }
}
