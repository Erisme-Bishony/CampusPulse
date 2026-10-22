package com.campuspulse.server.auth;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.server.auth.dto.UpdateInterestsRequest;
import com.campuspulse.server.auth.dto.UpdateProfileRequest;
import com.campuspulse.server.auth.dto.UserResponse;
import com.campuspulse.server.common.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(authService.me(userId));
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateProfile(@RequestAttribute("userId") Long userId,
                                                   @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(authService.updateProfile(userId, request));
    }

    @GetMapping("/me/interests")
    public ApiResponse<List<Integer>> interests(@RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(authService.me(userId).tagIds());
    }

    @PutMapping("/me/interests")
    public ApiResponse<UserResponse> updateInterests(@RequestAttribute("userId") Long userId,
                                                     @Valid @RequestBody UpdateInterestsRequest request) {
        return ApiResponse.ok(authService.updateInterests(userId, request));
    }
}
