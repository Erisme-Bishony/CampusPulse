package com.campuspulse.server.behavior.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record BehaviorResponse(
        Long id,
        Long activityId,
        String type,
        Integer durationSeconds,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {
}
