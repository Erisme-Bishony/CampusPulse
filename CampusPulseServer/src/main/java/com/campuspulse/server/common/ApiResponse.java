package com.campuspulse.server.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一响应结构：code=0 表示成功，非 0 为业务错误码。
 */
public record ApiResponse<T>(int code, String message, T data, String timestamp) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data, now());
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        String text = (message == null || message.isBlank()) ? errorCode.getDefaultMessage() : message;
        return new ApiResponse<>(errorCode.getCode(), text, null, now());
    }

    private static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
