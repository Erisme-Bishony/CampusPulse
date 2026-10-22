package com.campuspulse.server.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.campuspulse.server.common.BizException;
import com.campuspulse.server.common.ErrorCode;
import com.campuspulse.server.common.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        JwtUtil.TokenPayload payload = jwtUtil.parse(header.substring(BEARER_PREFIX.length()).trim());
        request.setAttribute("userId", payload.userId());
        request.setAttribute("role", payload.role());
        return true;
    }
}
