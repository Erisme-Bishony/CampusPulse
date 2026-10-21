package com.campuspulse.server.activity.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.campuspulse.server.tag.dto.TagResponse;
import com.fasterxml.jackson.annotation.JsonFormat;

public record ActivitySummaryResponse(
        Long id,
        String title,
        String type,
        String location,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime signupDeadline,
        Integer capacity,
        Integer signupCount,
        Integer remaining,
        String coverUrl,
        String status,
        List<TagResponse> tags) {
}
