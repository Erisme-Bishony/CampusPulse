package com.campuspulse.server.behavior;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.server.behavior.dto.BehaviorRequest;
import com.campuspulse.server.behavior.dto.BehaviorResponse;
import com.campuspulse.server.common.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/behaviors")
public class BehaviorController {

    private final BehaviorService behaviorService;

    public BehaviorController(BehaviorService behaviorService) {
        this.behaviorService = behaviorService;
    }

    @PostMapping
    public ApiResponse<BehaviorResponse> record(@RequestAttribute("userId") Long userId,
                                                @Valid @RequestBody BehaviorRequest request) {
        return ApiResponse.ok(behaviorService.record(userId, request));
    }
}
