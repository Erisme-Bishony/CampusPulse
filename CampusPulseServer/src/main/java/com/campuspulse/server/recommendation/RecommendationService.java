package com.campuspulse.server.recommendation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import com.campuspulse.server.domain.Behavior;
import com.campuspulse.server.domain.Recommendation;
import com.campuspulse.server.domain.Registration;
import com.campuspulse.server.domain.UserTag;
import com.campuspulse.server.recommendation.dto.RecommendationResponse;
import com.campuspulse.server.repository.ActivityRepository;
import com.campuspulse.server.repository.BehaviorRepository;
import com.campuspulse.server.repository.RecommendationRepository;
import com.campuspulse.server.repository.RegistrationRepository;
import com.campuspulse.server.repository.UserTagRepository;

@Service
public class RecommendationService {

    private static final int MAX_LIMIT = 20;
    private static final int MAX_NEIGHBORS = 20;
    private static final double CF_REASON_THRESHOLD = 0.12;
    private static final String PUBLISHED = "PUBLISHED";
    private static final String REGISTERED = "REGISTERED";
    private static final String VIEW = "VIEW";
    private static final String SHARE = "SHARE";
    private static final String DURATION = "DURATION";

    private final ActivityRepository activityRepository;
    private final ActivityService activityService;
    private final UserTagRepository userTagRepository;
    private final RegistrationRepository registrationRepository;
    private final BehaviorRepository behaviorRepository;
    private final RecommendationRepository recommendationRepository;

    public RecommendationService(ActivityRepository activityRepository,
                                 ActivityService activityService,
                                 UserTagRepository userTagRepository,
                                 RegistrationRepository registrationRepository,
                                 BehaviorRepository behaviorRepository,
                                 RecommendationRepository recommendationRepository) {
        this.activityRepository = activityRepository;
        this.activityService = activityService;
        this.userTagRepository = userTagRepository;
        this.registrationRepository = registrationRepository;
        this.behaviorRepository = behaviorRepository;
        this.recommendationRepository = recommendationRepository;
    }

    /**
     * 推荐分数：
     * 有行为且能找到相似用户时：0.4 内容 + 0.4 协同过滤 + 0.2 热度；
     * 没有协同过滤时回退到 0.7 内容 + 0.3 热度；
     * 没有兴趣标签时用热度，有行为时叠加协同过滤。
     *
     * 行为偏好：报名 3 分，浏览最多 4 次 × 0.75，分享最多 2 次 × 1.5，
     * 停留时长按分钟折算、最多 3 分；每个用户再按最大值归一化。
     */
    @Transactional
    public List<RecommendationResponse> list(Long userId, int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BizException(ErrorCode.BAD_REQUEST, "推荐数量必须在 1 到 20 之间");
        }

        Set<Integer> interestTagIds = userTagRepository.findByUserId(userId).stream()
                .map(UserTag::getTagId)
                .collect(Collectors.toSet());
        List<Registration> allRegistrations = registrationRepository.findByStatus(REGISTERED);
        Set<Long> registeredActivityIds = allRegistrations.stream()
                .filter(registration -> userId.equals(registration.getUserId()))
                .map(Registration::getActivityId)
                .collect(Collectors.toSet());

        Map<Long, Map<Long, Double>> preferences = buildPreferences(
                allRegistrations, behaviorRepository.findAll());
        Map<Long, Double> currentPreferences = preferences.getOrDefault(userId, Map.of());
        List<Neighbor> neighbors = neighbors(userId, currentPreferences, preferences);

        List<Activity> candidates = activityRepository
                .findTop200ByStatusAndStartTimeAfterOrderByStartTimeAsc(PUBLISHED, LocalDateTime.now()).stream()
                .filter(activity -> !registeredActivityIds.contains(activity.getId()))
                .toList();

        Map<Long, ActivitySummaryResponse> summaries = activityService.summarize(candidates);
        Map<Long, Double> cfScores = new HashMap<>();
        Map<Long, Integer> cfContributors = new HashMap<>();
        scoreCollaborative(candidates, neighbors, cfScores, cfContributors);

        List<ScoredActivity> scored = new ArrayList<>();
        for (Activity activity : candidates) {
            ActivitySummaryResponse summary = summaries.get(activity.getId());
            Set<Integer> activityTagIds = summary.tags().stream()
                    .map(TagResponse::id)
                    .collect(Collectors.toSet());

            double contentScore = jaccard(interestTagIds, activityTagIds);
            double popularityScore = popularity(activity);
            double cfScore = cfScores.getOrDefault(activity.getId(), 0.0);
            int contributorCount = cfContributors.getOrDefault(activity.getId(), 0);
            boolean hasCf = cfScore > 0 && contributorCount > 0;

            double score;
            String algorithm;
            if (hasCf && !interestTagIds.isEmpty()) {
                score = 0.4 * contentScore + 0.4 * cfScore + 0.2 * popularityScore;
                algorithm = "COLLABORATIVE_CONTENT";
            } else if (hasCf) {
                score = 0.6 * cfScore + 0.4 * popularityScore;
                algorithm = "COLLABORATIVE";
            } else if (!interestTagIds.isEmpty()) {
                score = 0.7 * contentScore + 0.3 * popularityScore;
                algorithm = "CONTENT_POPULARITY";
            } else {
                score = popularityScore;
                algorithm = "POPULARITY";
            }
            score = Math.round(Math.min(1.0, Math.max(0.0, score)) * 10000.0) / 10000.0;

            String reason = reasonFor(summary, interestTagIds, activity, cfScore, contributorCount);
            scored.add(new ScoredActivity(
                    summary, score, reason, algorithm, activity.getStartTime(), activity.getId()));
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
            List<Recommendation> rows = top.stream()
                    .map(item -> new Recommendation(
                            userId,
                            item.activity().id(),
                            BigDecimal.valueOf(item.score()).setScale(4, RoundingMode.HALF_UP),
                            item.reason(),
                            item.algorithm(),
                            generatedAt))
                    .toList();
            recommendationRepository.saveAll(rows);
        }

        return top.stream()
                .map(item -> new RecommendationResponse(item.activity(), item.score(), item.reason()))
                .toList();
    }

    /**
     * 用报名、浏览、分享和停留时长构建隐式偏好；每个用户按最大值归一化。
     */
    private Map<Long, Map<Long, Double>> buildPreferences(List<Registration> registrations,
                                                          List<Behavior> behaviors) {
        Map<Long, Map<Long, Double>> raw = new HashMap<>();
        Map<Long, Map<Long, Integer>> viewCounts = new HashMap<>();
        Map<Long, Map<Long, Integer>> shareCounts = new HashMap<>();
        Map<Long, Map<Long, Long>> durationSeconds = new HashMap<>();

        for (Registration registration : registrations) {
            add(raw, registration.getUserId(), registration.getActivityId(), 3.0);
        }
        for (Behavior behavior : behaviors) {
            Long activityId = behavior.getActivityId();
            Long behaviorUserId = behavior.getUserId();
            String type = behavior.getType() == null ? "" : behavior.getType();
            switch (type) {
                case VIEW -> viewCounts
                        .computeIfAbsent(behaviorUserId, key -> new HashMap<>())
                        .merge(activityId, 1, Integer::sum);
                case SHARE -> shareCounts
                        .computeIfAbsent(behaviorUserId, key -> new HashMap<>())
                        .merge(activityId, 1, Integer::sum);
                case DURATION -> {
                    long seconds = behavior.getDurationSeconds() == null
                            ? 0
                            : behavior.getDurationSeconds();
                    if (seconds > 0) {
                        durationSeconds
                                .computeIfAbsent(behaviorUserId, key -> new HashMap<>())
                                .merge(activityId, seconds, Long::sum);
                    }
                }
                default -> {
                    // 未知行为不参与推荐，避免脏数据放大分数。
                }
            }
        }

        viewCounts.forEach((behaviorUserId, byActivity) -> byActivity.forEach((activityId, count) ->
                add(raw, behaviorUserId, activityId, Math.min(count, 4) * 0.75)));
        shareCounts.forEach((behaviorUserId, byActivity) -> byActivity.forEach((activityId, count) ->
                add(raw, behaviorUserId, activityId, Math.min(count, 2) * 1.5)));
        durationSeconds.forEach((behaviorUserId, byActivity) -> byActivity.forEach((activityId, seconds) ->
                add(raw, behaviorUserId, activityId, Math.min(seconds / 60.0, 3.0))));

        Map<Long, Map<Long, Double>> normalized = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : raw.entrySet()) {
            double max = entry.getValue().values().stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .orElse(0.0);
            if (max <= 0) {
                continue;
            }
            Map<Long, Double> byActivity = new HashMap<>();
            entry.getValue().forEach((activityId, value) -> byActivity.put(activityId, value / max));
            normalized.put(entry.getKey(), byActivity);
        }
        return normalized;
    }

    private void add(Map<Long, Map<Long, Double>> values, Long userId, Long activityId, double value) {
        values.computeIfAbsent(userId, key -> new HashMap<>())
                .merge(activityId, value, Double::sum);
    }

    private List<Neighbor> neighbors(Long userId,
                                     Map<Long, Double> currentPreferences,
                                     Map<Long, Map<Long, Double>> preferences) {
        if (currentPreferences.isEmpty()) {
            return List.of();
        }
        List<Neighbor> result = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : preferences.entrySet()) {
            if (entry.getKey().equals(userId)) {
                continue;
            }
            double similarity = cosine(currentPreferences, entry.getValue());
            if (similarity > 0.0001) {
                result.add(new Neighbor(entry.getKey(), similarity, entry.getValue()));
            }
        }
        result.sort(Comparator.comparingDouble(Neighbor::similarity).reversed());
        return result.size() <= MAX_NEIGHBORS ? result : result.subList(0, MAX_NEIGHBORS);
    }

    private void scoreCollaborative(List<Activity> candidates,
                                    List<Neighbor> neighbors,
                                    Map<Long, Double> scores,
                                    Map<Long, Integer> contributors) {
        if (neighbors.isEmpty()) {
            return;
        }
        for (Activity activity : candidates) {
            double weighted = 0;
            double similaritySum = 0;
            int count = 0;
            for (Neighbor neighbor : neighbors) {
                Double preference = neighbor.preferences().get(activity.getId());
                if (preference == null || preference <= 0) {
                    continue;
                }
                weighted += neighbor.similarity() * preference;
                similaritySum += neighbor.similarity();
                count++;
            }
            if (similaritySum > 0) {
                scores.put(activity.getId(), weighted / similaritySum);
                contributors.put(activity.getId(), count);
            }
        }
    }

    private double cosine(Map<Long, Double> left, Map<Long, Double> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        double dot = 0;
        double leftNorm = 0;
        for (Map.Entry<Long, Double> entry : left.entrySet()) {
            double value = entry.getValue();
            leftNorm += value * value;
            Double rightValue = right.get(entry.getKey());
            if (rightValue != null) {
                dot += value * rightValue;
            }
        }
        double rightNorm = right.values().stream().mapToDouble(value -> value * value).sum();
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / Math.sqrt(leftNorm * rightNorm);
    }

    private double popularity(Activity activity) {
        double signupScore = Math.min(activity.getSignupCount() / 50.0, 1.0);
        double viewScore = Math.min(activity.getViewCount() / 200.0, 1.0);
        double shareScore = Math.min(activity.getShareCount() / 50.0, 1.0);
        return 0.5 * signupScore + 0.3 * viewScore + 0.2 * shareScore;
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

    private String reasonFor(ActivitySummaryResponse summary,
                             Set<Integer> interestTagIds,
                             Activity activity,
                             double cfScore,
                             int contributorCount) {
        if (cfScore >= CF_REASON_THRESHOLD && contributorCount > 0) {
            return "和你兴趣相似的用户也关注了";
        }
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

    private record Neighbor(Long userId, double similarity, Map<Long, Double> preferences) {
    }

    private record ScoredActivity(ActivitySummaryResponse activity, double score, String reason,
                                  String algorithm, LocalDateTime startTime, Long activityId) {
    }
}
