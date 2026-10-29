package com.campuspulse.server.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.server.domain.Activity;
import com.campuspulse.server.domain.ActivityTag;
import com.campuspulse.server.domain.Tag;
import com.campuspulse.server.repository.ActivityRepository;
import com.campuspulse.server.repository.ActivityTagRepository;
import com.campuspulse.server.repository.TagRepository;

@Service
public class AdminActivityService {

    private static final List<String> TYPES = List.of("讲座", "比赛", "文艺", "志愿", "体育", "社团", "其他");
    private static final List<String> STATUSES = List.of("PUBLISHED", "DRAFT");

    private final ActivityRepository activityRepository;
    private final ActivityTagRepository activityTagRepository;
    private final TagRepository tagRepository;

    public AdminActivityService(ActivityRepository activityRepository,
                                ActivityTagRepository activityTagRepository,
                                TagRepository tagRepository) {
        this.activityRepository = activityRepository;
        this.activityTagRepository = activityTagRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional
    public Activity save(AdminActivityForm form) {
        String title = required(form.getTitle(), "活动标题");
        String description = required(form.getDescription(), "活动介绍");
        String location = required(form.getLocation(), "活动地点");
        String type = required(form.getType(), "活动分类");
        if (!TYPES.contains(type)) {
            throw new IllegalArgumentException("活动分类不在允许范围内");
        }
        if (!STATUSES.contains(form.getStatus())) {
            throw new IllegalArgumentException("活动状态只能是 PUBLISHED 或 DRAFT");
        }
        LocalDateTime startTime = parseTime(form.getStartTime(), "开始时间");
        LocalDateTime endTime = parseTime(form.getEndTime(), "结束时间");
        LocalDateTime signupDeadline = parseTime(form.getSignupDeadline(), "报名截止时间");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        if (signupDeadline.isAfter(startTime)) {
            throw new IllegalArgumentException("报名截止时间不能晚于活动开始时间");
        }
        int capacity = form.getCapacity() == null ? 0 : form.getCapacity();
        if (capacity < 1) {
            throw new IllegalArgumentException("活动容量至少为 1");
        }

        Activity activity = form.getId() == null
                ? new Activity()
                : activityRepository.findById(form.getId())
                        .orElseThrow(() -> new IllegalArgumentException("活动不存在"));
        int signupCount = activity.getSignupCount() == null ? 0 : activity.getSignupCount();
        if (capacity < signupCount) {
            throw new IllegalArgumentException("容量不能小于当前报名人数 " + signupCount);
        }

        activity.setTitle(title);
        activity.setDescription(description);
        activity.setLocation(location);
        activity.setType(type);
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        activity.setSignupDeadline(signupDeadline);
        activity.setCapacity(capacity);
        activity.setCoverUrl(blankToNull(form.getCoverUrl(), 255));
        activity.setOrganizer(blankToNull(form.getOrganizer(), 100));
        activity.setStatus(form.getStatus());
        LocalDateTime now = LocalDateTime.now();
        activity.setUpdatedAt(now);
        if (activity.getId() == null) {
            activity.setViewCount(0);
            activity.setSignupCount(0);
            activity.setShareCount(0);
            activity.setCreatedAt(now);
        }
        activityRepository.save(activity);

        List<Integer> tagIds = form.getTagIds() == null ? List.of() : form.getTagIds().stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (!tagIds.isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(tagIds);
            if (tags.size() != tagIds.size()) {
                throw new IllegalArgumentException("存在无效的活动标签");
            }
        }
        activityTagRepository.deleteByActivityId(activity.getId());
        activityTagRepository.saveAll(tagIds.stream()
                .map(tagId -> new ActivityTag(activity.getId(), tagId))
                .toList());
        return activity;
    }

    @Transactional
    public void toggleStatus(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("活动不存在"));
        activity.setStatus("PUBLISHED".equals(activity.getStatus()) ? "DRAFT" : "PUBLISHED");
        activity.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<Integer> tagIdsOf(Long activityId) {
        List<Integer> ids = new ArrayList<>();
        for (ActivityTag link : activityTagRepository.findByIdActivityIdIn(List.of(activityId))) {
            ids.add(link.getId().getTagId());
        }
        return ids;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return value.trim();
    }

    private String blankToNull(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException("字段长度不能超过 " + maxLength);
        }
        return trimmed;
    }

    private LocalDateTime parseTime(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(field + "格式不正确，请使用日期时间选择器");
        }
    }
}
