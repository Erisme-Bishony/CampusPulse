package com.campuspulse.server.recommendation.dto;

import com.campuspulse.server.activity.dto.ActivitySummaryResponse;

public record RecommendationResponse(
        ActivitySummaryResponse activity,
        double score,
        String reason) {
}
