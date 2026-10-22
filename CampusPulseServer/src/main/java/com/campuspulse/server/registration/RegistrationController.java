package com.campuspulse.server.registration;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.server.common.ApiResponse;
import com.campuspulse.server.registration.dto.MyRegistrationResponse;
import com.campuspulse.server.registration.dto.RegistrationResponse;
import com.campuspulse.server.registration.dto.RegistrationStatusResponse;

@RestController
@RequestMapping("/api/v1")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/activities/{id}/registrations")
    public ApiResponse<RegistrationResponse> register(@RequestAttribute("userId") Long userId,
                                                      @PathVariable Long id) {
        return ApiResponse.ok(registrationService.register(userId, id));
    }

    @DeleteMapping("/activities/{id}/registrations")
    public ApiResponse<Void> cancel(@RequestAttribute("userId") Long userId,
                                    @PathVariable Long id) {
        registrationService.cancel(userId, id);
        return ApiResponse.ok();
    }

    @GetMapping("/activities/{id}/registration-status")
    public ApiResponse<RegistrationStatusResponse> status(@RequestAttribute("userId") Long userId,
                                                          @PathVariable Long id) {
        return ApiResponse.ok(registrationService.status(userId, id));
    }

    @GetMapping("/users/me/registrations")
    public ApiResponse<List<MyRegistrationResponse>> mine(@RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(registrationService.mine(userId));
    }
}
