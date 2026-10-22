package com.campuspulse.server.activity;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.server.activity.dto.ActivityDetailResponse;
import com.campuspulse.server.activity.dto.ActivitySummaryResponse;
import com.campuspulse.server.common.ApiResponse;
import com.campuspulse.server.common.PageResponse;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ActivitySummaryResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer tagId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(activityService.list(keyword, tagId, type, date, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActivityDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(activityService.detail(id));
    }
}
