package com.assignments.rbac.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String error;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(false, error, null);
    }

    public static <T> ApiResponse<T> error(String error, T data) {
        return new ApiResponse<>(false, error, data);
    }
}

