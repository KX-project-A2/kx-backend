package com.example.kxbackend.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String message,
        T data,
        String code
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>( "success", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>( message, data, null);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>( message, null, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>( message, null, code);
    }
}
