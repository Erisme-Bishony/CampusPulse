package com.campuspulse.server.common;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码与 HTTP 状态的唯一映射，新增错误码时同步更新 docs/API.md。
 */
public enum ErrorCode {

    BAD_REQUEST(40001, HttpStatus.BAD_REQUEST, "参数校验失败"),
    UNAUTHORIZED(40101, HttpStatus.UNAUTHORIZED, "未登录或登录已过期"),
    BAD_CREDENTIALS(40102, HttpStatus.UNAUTHORIZED, "用户名或密码错误"),
    FORBIDDEN(40301, HttpStatus.FORBIDDEN, "无权限访问"),
    NOT_FOUND(40401, HttpStatus.NOT_FOUND, "资源不存在"),
    USERNAME_EXISTS(40901, HttpStatus.CONFLICT, "用户名已存在"),
    ALREADY_REGISTERED(40902, HttpStatus.CONFLICT, "已报名，不能重复报名"),
    ACTIVITY_FULL(40903, HttpStatus.CONFLICT, "活动名额已满"),
    REGISTRATION_CLOSED(40904, HttpStatus.CONFLICT, "报名已截止或活动已结束"),
    INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final int code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
