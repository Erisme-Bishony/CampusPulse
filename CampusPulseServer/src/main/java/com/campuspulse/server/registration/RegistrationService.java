package com.campuspulse.server.registration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.server.common.BizException;
import com.campuspulse.server.common.ErrorCode;
import com.campuspulse.server.domain.Activity;
import com.campuspulse.server.domain.Registration;
import com.campuspulse.server.registration.dto.ActivityBriefResponse;
import com.campuspulse.server.registration.dto.MyRegistrationResponse;
import com.campuspulse.server.registration.dto.RegistrationResponse;
import com.campuspulse.server.registration.dto.RegistrationStatusResponse;
import com.campuspulse.server.repository.ActivityRepository;
import com.campuspulse.server.repository.RegistrationRepository;

@Service
public class RegistrationService {

    private static final String REGISTERED = "REGISTERED";
    private static final String CANCELLED = "CANCELLED";
    private static final String NONE = "NONE";
    private static final String PUBLISHED = "PUBLISHED";

    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;

    public RegistrationService(ActivityRepository activityRepository,
                               RegistrationRepository registrationRepository) {
        this.activityRepository = activityRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional
    public RegistrationResponse register(Long userId, Long activityId) {
        Activity activity = lockActivity(activityId);
        requirePublished(activity);

        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(activity.getSignupDeadline()) || !now.isBefore(activity.getEndTime())) {
            throw new BizException(ErrorCode.REGISTRATION_CLOSED, "报名已截止或活动已结束");
        }

        Registration registration = registrationRepository.findByUserIdAndActivityId(userId, activityId)
                .orElse(null);
        if (registration != null && REGISTERED.equals(registration.getStatus())) {
            throw new BizException(ErrorCode.ALREADY_REGISTERED, "已报名，不能重复报名");
        }

        int capacity = activity.getCapacity() == null ? 0 : activity.getCapacity();
        int signupCount = activity.getSignupCount() == null ? 0 : activity.getSignupCount();
        if (signupCount >= capacity) {
            throw new BizException(ErrorCode.ACTIVITY_FULL, "活动名额已满");
        }

        if (registration == null) {
            registration = new Registration();
            registration.setUserId(userId);
            registration.setActivityId(activityId);
        }
        registration.setStatus(REGISTERED);
        registration.setCreatedAt(now);
        registration.setCancelledAt(null);

        activity.setSignupCount(signupCount + 1);
        activityRepository.save(activity);
        Registration saved = registrationRepository.save(registration);
        return toResponse(saved);
    }

    @Transactional
    public void cancel(Long userId, Long activityId) {
        Activity activity = lockActivity(activityId);
        Registration registration = registrationRepository.findByUserIdAndActivityId(userId, activityId)
                .orElse(null);
        if (registration == null || !REGISTERED.equals(registration.getStatus())) {
            return;
        }

        registration.setStatus(CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        int signupCount = activity.getSignupCount() == null ? 0 : activity.getSignupCount();
        activity.setSignupCount(Math.max(0, signupCount - 1));
        activityRepository.save(activity);
        registrationRepository.save(registration);
    }

    @Transactional(readOnly = true)
    public RegistrationStatusResponse status(Long userId, Long activityId) {
        if (!activityRepository.existsById(activityId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        return registrationRepository.findByUserIdAndActivityId(userId, activityId)
                .map(registration -> new RegistrationStatusResponse(
                        REGISTERED.equals(registration.getStatus()), registration.getStatus()))
                .orElseGet(() -> new RegistrationStatusResponse(false, NONE));
    }

    @Transactional(readOnly = true)
    public List<MyRegistrationResponse> mine(Long userId) {
        List<Registration> registrations = registrationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (registrations.isEmpty()) {
            return List.of();
        }

        List<Long> activityIds = registrations.stream()
                .map(Registration::getActivityId)
                .distinct()
                .toList();
        Map<Long, Activity> activityById = activityRepository.findAllById(activityIds).stream()
                .collect(Collectors.toMap(Activity::getId, Function.identity()));

        return registrations.stream()
                .filter(registration -> activityById.containsKey(registration.getActivityId()))
                .map(registration -> new MyRegistrationResponse(
                        registration.getId(),
                        registration.getStatus(),
                        registration.getCreatedAt(),
                        toBrief(activityById.get(registration.getActivityId()))))
                .toList();
    }

    private Activity lockActivity(Long activityId) {
        return activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "活动不存在"));
    }

    private void requirePublished(Activity activity) {
        if (!PUBLISHED.equals(activity.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
    }

    private RegistrationResponse toResponse(Registration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getActivityId(),
                registration.getStatus(),
                registration.getCreatedAt());
    }

    private ActivityBriefResponse toBrief(Activity activity) {
        return new ActivityBriefResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getLocation(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getStatus());
    }
}
