package com.campuspulse.server.auth.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度需为 3-50 个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需为 6-64 个字符")
        String password,

        @NotBlank(message = "请再次输入密码")
        String confirmPassword,

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱过长")
        String email,

        @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
        String phone,

        List<Integer> tagIds) {
}
