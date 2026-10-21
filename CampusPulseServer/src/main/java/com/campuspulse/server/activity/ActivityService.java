package com.campuspulse.server.activity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.server.activity.dto.ActivityDetailResponse;
import com.campuspulse.server.activity.dto.ActivitySummaryResponse;
import com.campuspulse.server.common.BizException;
import com.campuspulse.server.common.ErrorCode;
import com.campuspulse.server.common.PageResponse;
import com.campuspulse.server.domain.Activity;
import com.campuspulse.server.domain.ActivityTag;
import com.campuspulse.server.domain.Tag;
import com.campuspulse.server.repository.ActivityRepository;
import com.campuspulse.server.repository.ActivityTagRepository;
import com.campuspulse.server.repository.TagRepository;
import com.campuspulse.server.tag.dto.TagResponse;

@Service
public class ActivityService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ActivityRepository activityRepository;
    private final ActivityTagRepository activityTagRepository;
    private final TagRepository tagRepository;

    public ActivityService(ActivityRepository activityRepository,
                           ActivityTagRepository activityTagRepository,
                           TagRepository tagRepository) {
        this.activityRepository = activityRepository;
        this.activityTagRepository = activityTagRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivitySummaryResponse> list(String keyword,
                                                      Integer tagId,
                                                      String type,
                                                      LocalDate date,
                                                      int page,
                                                      int size) {
        validatePage(page, size);
        String normalizedKeyword = normalizeKeyword(keyword);
        int normalizedSize = Math.min(size, MAX_PAGE_SIZE);

        Page<Activity> result = activityRepository.findAll(
                ActivitySpecifications.publishedFilter(normalizedKeyword, tagId, type, date),
                PageRequest.of(page - 1, normalizedSize,
                        Sort.by(Sort.Direction.ASC, "startTime").and(Sort.by(Sort.Direction.ASC, "id"))));

        Map<Long, List<TagResponse>> tagsByActivity = loadTags(result.getContent());
        List<ActivitySummaryResponse> items = result.getContent().stream()
                .map(activity -> toSummary(activity, tagsByActivity.getOrDefault(activity.getId(), List.of())))
                .toList();

        return new PageResponse<>(items, result.getTotalElements(), page, normalizedSize, result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ActivityDetailResponse detail(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .filter(found -> "PUBLISHED".equals(found.getStatus()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "活动不存在"));
        List<TagResponse> tags = loadTags(List.of(activity)).getOrDefault(activity.getId(), List.of());
        return toDetail(activity, tags);
    }

    private void validatePage(int page, int size) {
        if (page < 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "页码从 1 开始");
        }
        if (size < 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "每页数量至少为 1");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.codePointCount(0, trimmed.length()) < 2) {
            throw new BizException(ErrorCode.BAD_REQUEST, "关键词至少输入 2 个字");
        }
        return trimmed;
    }

    private Map<Long, List<TagResponse>> loadTags(List<Activity> activities) {
        if (activities.isEmpty()) {
            return Map.of();
        }
        List<Long> activityIds = activities.stream().map(Activity::getId).toList();
        List<ActivityTag> links = activityTagRepository.findByIdActivityIdIn(activityIds);
        if (links.isEmpty()) {
            return Map.of();
        }

        Set<Integer> tagIds = links.stream()
                .map(link -> link.getId().getTagId())
                .collect(Collectors.toSet());
        List<Tag> tags = tagRepository.findAllById(tagIds);
        Map<Integer, TagResponse> tagById = new HashMap<>();
        Map<Integer, Integer> orderById = new HashMap<>();
        for (Tag tag : tags) {
            tagById.put(tag.getId(), new TagResponse(tag.getId(), tag.getName(), tag.getColor()));
            orderById.put(tag.getId(), tag.getSortOrder());
        }

        Map<Long, List<TagResponse>> result = new LinkedHashMap<>();
        for (ActivityTag link : links) {
            TagResponse tag = tagById.get(link.getId().getTagId());
            if (tag != null) {
                result.computeIfAbsent(link.getId().getActivityId(), key -> new ArrayList<>()).add(tag);
            }
        }
        result.values().forEach(list -> list.sort(
                Comparator.comparingInt(tag -> orderById.getOrDefault(tag.id(), Integer.MAX_VALUE))));
        return result;
    }

    private ActivitySummaryResponse toSummary(Activity activity, List<TagResponse> tags) {
        return new ActivitySummaryResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getType(),
                activity.getLocation(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getSignupDeadline(),
                activity.getCapacity(),
                activity.getSignupCount(),
                remaining(activity),
                activity.getCoverUrl(),
                activity.getStatus(),
                tags);
    }

    private ActivityDetailResponse toDetail(Activity activity, List<TagResponse> tags) {
        return new ActivityDetailResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getType(),
                activity.getLocation(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getSignupDeadline(),
                activity.getCapacity(),
                activity.getSignupCount(),
                remaining(activity),
                activity.getCoverUrl(),
                activity.getStatus(),
                tags,
                activity.getDescription(),
                activity.getOrganizer(),
                activity.getViewCount(),
                activity.getShareCount(),
                activity.getCreatedAt(),
                activity.getUpdatedAt());
    }

    private int remaining(Activity activity) {
        int capacity = activity.getCapacity() == null ? 0 : activity.getCapacity();
        int signupCount = activity.getSignupCount() == null ? 0 : activity.getSignupCount();
        return Math.max(0, capacity - signupCount);
    }
}
