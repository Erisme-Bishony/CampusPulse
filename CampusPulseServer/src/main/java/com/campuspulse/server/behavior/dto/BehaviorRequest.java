package com.campuspulse.server.behavior.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BehaviorRequest(
        @NotNull(message = "活动 ID 不能为空") Long activityId,
        @NotBlank(message = "行为类型不能为空") String type,
        @Min(value = 0, message = "停留时长不能为负数") Integer durationSeconds) {
}
