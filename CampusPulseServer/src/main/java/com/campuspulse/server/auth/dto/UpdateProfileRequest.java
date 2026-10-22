package com.campuspulse.server.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Email(message = "邮箱格式不正确") @Size(max = 100) String email,
        @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确") String phone) {
}
