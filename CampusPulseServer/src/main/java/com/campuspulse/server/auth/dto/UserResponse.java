package com.campuspulse.server.auth.dto;

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String email,
        String phone,
        String role,
        List<Integer> tagIds) {
}
