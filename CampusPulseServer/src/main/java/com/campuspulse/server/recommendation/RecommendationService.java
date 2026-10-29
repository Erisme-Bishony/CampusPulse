package com.campuspulse.server.recommendation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.server.activity.ActivityService;
import com.campuspulse.server.activity.dto.ActivitySummaryResponse;
import com.campuspulse.server.tag.dto.TagResponse;
import com.campuspulse.server.common.BizException;
import com.campuspulse.server.common.ErrorCode;
import com.campuspulse.server.domain.Activity;
import com.campuspulse.server.domain.Recommendation;
import com.campuspulse.server.domain.Registration;
import com.campuspulse.server.domain.UserTag;
import com.campuspulse.server.recommendation.dto.RecommendationResponse;
import com.campuspulse.server.repository.ActivityRepository;
import com.campuspulse.server.repository.RecommendationRepository;
import com.campuspulse.server.repository.RegistrationRepository;
import com.campuspulse.server.repository.UserTagRepository;

@Service
public class RecommendationService {

    private static final int MAX_LIMIT = 20;
    private static final int CANDIDATE_LIMIT = 200;
    private static final String PUBLISHED = "PUBLISHED";
    private static final String REGISTERED = "REGISTERED";

    private final ActivityRepository activityRepository;
    private final ActivityService activityService;
    private final UserTagRepository userTagRepository;
    private final RegistrationRepository registrationRepository;
    private final RecommendationRepository recommendationRepository;

    public RecommendationService(ActivityRepository activityRepository,
                                 ActivityService activityService,
                                 UserTagRepository userTagRepository,
                                 RegistrationRepository registrationRepository,
                                 RecommendationRepository recommendationRepository) {
        this.activityRepository = activityRepository;
        this.activityService = activityService;
        this.userTagRepository = userTagRepository;
        this.registrationRepository = registrationRepository;
        this.recommendationRepository = recommendationRepository;
    }

    /**
     * 推荐分数：有标签时 0.7 内容相似度 + 0.3 热度；冷启动只有热度。
     * 内容相似度用用户兴趣标签与活动标签的 Jaccard 系数。
     */
    @Transactional
    public List<RecommendationResponse> list(Long userId, int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BizException(ErrorCode.BAD_REQUEST, "推荐数量必须在 1 到 20 之间");
        }

        Set<Integer> interestTagIds = userTagRepository.findByUserId(userId).stream()
                .map(UserTag::getTagId)
                .collect(Collectors.toSet());
        Set<Long> registeredActivityIds = registrationRepository
                .findByUserIdAndStatus(userId, REGISTERED).stream()
                .map(Registration::getActivityId)
                .collect(Collectors.toSet());

        List<Activity> candidates = activityRepository
                .findTop200ByStatusAndStartTimeAfterOrderByStartTimeAsc(PUBLISHED, LocalDateTime.now()).stream()
                .filter(activity -> !registeredActivityIds.contains(activity.getId()))
                .toList();

        Map<Long, ActivitySummaryResponse> summaries = activityService.summarize(candidates);
        List<ScoredActivity> scored = new ArrayList<>();
        for (Activity activity : candidates) {
            ActivitySummaryResponse summary = summaries.get(activity.getId());
            Set<Integer> activityTagIds = summary.tags().stream()
                    .map(TagResponse::id)
                    .collect(Collectors.toSet());

            double contentScore = jaccard(interestTagIds, activityTagIds);
            double popularityScore = popularity(activity);
            double score = interestTagIds.isEmpty()
                    ? popularityScore
                    : 0.7 * contentScore + 0.3 * popularityScore;
            score = Math.round(score * 10000.0) / 10000.0;

            String reason = reasonFor(summary, interestTagIds, activity);
            scored.add(new ScoredActivity(summary, score, reason, activity.getStartTime(), activity.getId()));
        }

        List<ScoredActivity> top = scored.stream()
                .sorted(Comparator.comparingDouble(ScoredActivity::score).reversed()
                        .thenComparing(ScoredActivity::startTime)
                        .thenComparing(ScoredActivity::activityId))
                .limit(limit)
                .toList();

        recommendationRepository.deleteByUserId(userId);
        if (!top.isEmpty()) {
            LocalDateTime generatedAt = LocalDateTime.now();
            String algorithm = interestTagIds.isEmpty() ? "POPULARITY" : "CONTENT_POPULARITY";
            List<Recommendation> rows = top.stream()
                    .map(item -> new Recommendation(
                            userId,
                            item.activity().id(),
                            BigDecimal.valueOf(item.score()).setScale(4, RoundingMode.HALF_UP),
                            item.reason(),
                            algorithm,
                            generatedAt))
                    .toList();
            recommendationRepository.saveAll(rows);
        }

        return top.stream()
                .map(item -> new RecommendationResponse(item.activity(), item.score(), item.reason()))
                .toList();
    }

    private double popularity(Activity activity) {
        double signupScore = Math.min(activity.getSignupCount() / 50.0, 1.0);
        double viewScore = Math.min(activity.getViewCount() / 200.0, 1.0);
        return 0.6 * signupScore + 0.4 * viewScore;
    }

    private double jaccard(Set<Integer> left, Set<Integer> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        Set<Integer> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        Set<Integer> union = new HashSet<>(left);
        union.addAll(right);
        return (double) intersection.size() / union.size();
    }

    private String reasonFor(ActivitySummaryResponse summary, Set<Integer> interestTagIds, Activity activity) {
        List<String> matchedTags = summary.tags().stream()
                .filter(tag -> interestTagIds.contains(tag.id()))
                .map(TagResponse::name)
                .limit(2)
                .toList();
        if (!matchedTags.isEmpty()) {
            return "匹配你的兴趣：" + String.join("、", matchedTags);
        }
        if (activity.getSignupCount() > 0) {
            return "近期热门活动";
        }
        return "新活动推荐";
    }

    private record ScoredActivity(ActivitySummaryResponse activity, double score, String reason,
                                  LocalDateTime startTime, Long activityId) {
    }
}
