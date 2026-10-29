package com.campuspulse.server.behavior;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.server.behavior.dto.BehaviorRequest;
import com.campuspulse.server.behavior.dto.BehaviorResponse;
import com.campuspulse.server.common.BizException;
import com.campuspulse.server.common.ErrorCode;
import com.campuspulse.server.domain.Activity;
import com.campuspulse.server.domain.Behavior;
import com.campuspulse.server.repository.ActivityRepository;
import com.campuspulse.server.repository.BehaviorRepository;

@Service
public class BehaviorService {

    private static final String PUBLISHED = "PUBLISHED";
    private static final String VIEW = "VIEW";
    private static final String SHARE = "SHARE";
    private static final String DURATION = "DURATION";
    private static final Set<String> TYPES = Set.of(VIEW, SHARE, DURATION);

    private final ActivityRepository activityRepository;
    private final BehaviorRepository behaviorRepository;

    public BehaviorService(ActivityRepository activityRepository,
                           BehaviorRepository behaviorRepository) {
        this.activityRepository = activityRepository;
        this.behaviorRepository = behaviorRepository;
    }

    /**
     * 记录一次行为。VIEW/SHARE 同时累加活动计数，DURATION 只保存停留时长。
     * 活动计数用原子 update，避免并发浏览时丢失增量。
     */
    @Transactional
    public BehaviorResponse record(Long userId, BehaviorRequest request) {
        String type = request.type().trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "行为类型只能是 VIEW、SHARE 或 DURATION");
        }

        Activity activity = activityRepository.findById(request.activityId())
                .filter(found -> PUBLISHED.equals(found.getStatus()))
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "活动不存在"));

        int durationSeconds = 0;
        if (DURATION.equals(type)) {
            if (request.durationSeconds() == null || request.durationSeconds() <= 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "停留时长必须大于 0 秒");
            }
            durationSeconds = request.durationSeconds();
        } else if (VIEW.equals(type)) {
            if (activityRepository.incrementViewCount(activity.getId()) == 0) {
                throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
            }
        } else if (activityRepository.incrementShareCount(activity.getId()) == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }

        Behavior behavior = new Behavior(userId, activity.getId(), type, durationSeconds, LocalDateTime.now());
        Behavior saved = behaviorRepository.save(behavior);
        return new BehaviorResponse(
                saved.getId(),
                saved.getActivityId(),
                saved.getType(),
                saved.getDurationSeconds(),
                saved.getCreatedAt());
    }
}
