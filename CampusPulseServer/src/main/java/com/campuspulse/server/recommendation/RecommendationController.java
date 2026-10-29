package com.campuspulse.server.recommendation;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.server.common.ApiResponse;
import com.campuspulse.server.recommendation.dto.RecommendationResponse;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ApiResponse<List<RecommendationResponse>> list(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(recommendationService.list(userId, limit));
    }
}
