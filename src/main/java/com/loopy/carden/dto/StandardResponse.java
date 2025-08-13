package com.loopy.carden.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> StandardResponse<T> success(T data) {
        return StandardResponse.<T>builder()
            .success(true)
            .data(data)
            .build();
    }

    public static <T> StandardResponse<T> success(String message, T data) {
        return StandardResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .build();
    }

    public static <T> StandardResponse<T> error(String message) {
        return StandardResponse.<T>builder()
            .success(false)
            .message(message)
            .build();
    }
}
