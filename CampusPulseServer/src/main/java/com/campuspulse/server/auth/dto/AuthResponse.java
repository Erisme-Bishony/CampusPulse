package com.campuspulse.server.auth.dto;

public record AuthResponse(String token, UserResponse user) {
}
