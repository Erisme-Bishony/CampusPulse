package com.campuspulse.server.admin;

import java.util.UUID;

import jakarta.servlet.http.HttpSession;

/**
 * 管理后台会话键与登录态判断。学生 JWT 与后台 Session 完全分开。
 */
public final class AdminSession {

    public static final String ADMIN_ID = "adminId";
    public static final String ADMIN_NAME = "adminName";
    public static final String CSRF_TOKEN = "csrfToken";

    private AdminSession() {
    }

    public static boolean isLoggedIn(HttpSession session) {
        return session != null && session.getAttribute(ADMIN_ID) instanceof Long;
    }

    public static Long adminId(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(ADMIN_ID);
        return value instanceof Long id ? id : null;
    }

    public static String adminName(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(ADMIN_NAME);
        return value instanceof String name ? name : "管理员";
    }

    public static String ensureCsrfToken(HttpSession session) {
        Object existing = session.getAttribute(CSRF_TOKEN);
        if (existing instanceof String token && !token.isBlank()) {
            return token;
        }
        String token = UUID.randomUUID().toString();
        session.setAttribute(CSRF_TOKEN, token);
        return token;
    }
}
