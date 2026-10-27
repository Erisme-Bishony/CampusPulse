package com.campuspulse.server.admin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 后台表单的轻量 CSRF 防护：GET 时生成会话 token，POST 时校验 _csrf 参数。
 * 登录页不在此拦截器范围内。
 */
@Component
public class AdminCsrfInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(true);
        String expected = AdminSession.ensureCsrfToken(session);

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String actual = request.getParameter("_csrf");
        if (!matches(expected, actual)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "表单已过期，请刷新页面后重试");
            return false;
        }
        return true;
    }

    private boolean matches(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
