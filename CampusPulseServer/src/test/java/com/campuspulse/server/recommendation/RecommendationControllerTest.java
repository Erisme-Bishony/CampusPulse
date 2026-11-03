package com.campuspulse.server.recommendation;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.campuspulse.server.common.JwtUtil;
import com.campuspulse.server.domain.Activity;
import com.campuspulse.server.domain.ActivityTag;
import com.campuspulse.server.domain.Behavior;
import com.campuspulse.server.domain.Registration;
import com.campuspulse.server.domain.Tag;
import com.campuspulse.server.domain.User;
import com.campuspulse.server.domain.UserTag;
import com.campuspulse.server.repository.ActivityRepository;
import com.campuspulse.server.repository.ActivityTagRepository;
import com.campuspulse.server.repository.BehaviorRepository;
import com.campuspulse.server.repository.RecommendationRepository;
import com.campuspulse.server.repository.RegistrationRepository;
import com.campuspulse.server.repository.TagRepository;
import com.campuspulse.server.repository.UserRepository;
import com.campuspulse.server.repository.UserTagRepository;

@SpringBootTest
@AutoConfigureMockMvc
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private UserTagRepository userTagRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private ActivityTagRepository activityTagRepository;
    @Autowired
    private RegistrationRepository registrationRepository;
    @Autowired
    private BehaviorRepository behaviorRepository;
    @Autowired
    private RecommendationRepository recommendationRepository;

    private User user;
    private String token;
    private LocalDateTime now;

    @BeforeEach
    void clean() {
        recommendationRepository.deleteAll();
        registrationRepository.deleteAll();
        behaviorRepository.deleteAll();
        activityTagRepository.deleteAll();
        activityRepository.deleteAll();
        userTagRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();

        now = LocalDateTime.now().withNano(0);
        user = new User();
        user.setUsername("recommend-" + System.nanoTime());
        user.setPasswordHash("x");
        user.setStatus((byte) 1);
        user = userRepository.save(user);
        token = jwtUtil.generate(user.getId(), "USER");
    }

    @Test
    void ranksContentMatchFirstAndExcludesRegisteredActivities() throws Exception {
        Tag lecture = tag("讲座");
        Tag sports = tag("运动");
        Tag art = tag("音乐");
        userTagRepository.save(new UserTag(user.getId(), lecture.getId()));

        Activity lectureActivity = activity("人工智能讲座", lecture, 10, 100, now.plusDays(2));
        Activity sportsActivity = activity("校园篮球赛", sports, 40, 50, now.plusDays(3));
        activity("草地音乐会", art, 0, 0, now.plusDays(4));
        Activity past = activity("昨天的旧讲座", lecture, 100, 500, now.minusDays(1));
        past.setStatus("PUBLISHED");
        activityRepository.save(past);

        Registration registration = new Registration();
        registration.setUserId(user.getId());
        registration.setActivityId(sportsActivity.getId());
        registration.setStatus("REGISTERED");
        registration.setCreatedAt(now);
        registrationRepository.save(registration);

        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.length()", is(2)))
                .andExpect(jsonPath("$.data[0].activity.title", is("人工智能讲座")))
                .andExpect(jsonPath("$.data[0].reason", containsString("讲座")))
                .andExpect(jsonPath("$.data[1].activity.title", is("草地音乐会")))
                .andExpect(jsonPath("$.data[1].reason", is("新活动推荐")));

        org.junit.jupiter.api.Assertions.assertEquals(2, recommendationRepository.findByUserIdOrderByScoreDesc(user.getId()).size());
        org.junit.jupiter.api.Assertions.assertEquals("CONTENT_POPULARITY",
                recommendationRepository.findByUserIdOrderByScoreDesc(user.getId()).get(0).getAlgorithm());
    }

    @Test
    void coldStartFallsBackToPopularity() throws Exception {
        Tag lecture = tag("讲座");
        Tag sports = tag("运动");
        activity("人工智能讲座", lecture, 10, 100, now.plusDays(2));
        activity("校园篮球赛", sports, 40, 50, now.plusDays(3));
        activity("草地音乐会", null, 0, 0, now.plusDays(4));

        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", is(3)))
                .andExpect(jsonPath("$.data[0].activity.title", is("校园篮球赛")))
                .andExpect(jsonPath("$.data[0].reason", is("近期热门活动")))
                .andExpect(jsonPath("$.data[1].activity.title", is("人工智能讲座")))
                .andExpect(jsonPath("$.data[2].activity.title", is("草地音乐会")))
                .andExpect(jsonPath("$.data[2].reason", is("新活动推荐")));

        org.junit.jupiter.api.Assertions.assertEquals("POPULARITY",
                recommendationRepository.findByUserIdOrderByScoreDesc(user.getId()).get(0).getAlgorithm());
    }

    @Test
    void returnsEmptyWhenAllCandidatesAreRegistered() throws Exception {
        Tag lecture = tag("讲座");
        Activity only = activity("唯一的讲座", lecture, 10, 100, now.plusDays(2));

        Registration registration = new Registration();
        registration.setUserId(user.getId());
        registration.setActivityId(only.getId());
        registration.setStatus("REGISTERED");
        registration.setCreatedAt(now);
        registrationRepository.save(registration);

        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", is(0)));

        org.junit.jupiter.api.Assertions.assertEquals(0, recommendationRepository.findByUserIdOrderByScoreDesc(user.getId()).size());
    }

    @Test
    void rejectsLimitOutsideRange() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(40001)));
    }

    @Test
    void collaborativeFilteringRanksActivitiesBySimilarUsers() throws Exception {
        Tag lecture = tag("讲座");
        userTagRepository.save(new UserTag(user.getId(), lecture.getId()));

        Activity historyX = activity("历史浏览X", lecture, 0, 0, now.minusDays(3));
        Activity historyY = activity("历史浏览Y", lecture, 0, 0, now.minusDays(2));
        Activity historyZ = activity("历史浏览Z", lecture, 0, 0, now.minusDays(1));
        behavior(user.getId(), historyX.getId(), "VIEW", 0);
        behavior(user.getId(), historyY.getId(), "VIEW", 0);
        behavior(user.getId(), historyZ.getId(), "VIEW", 0);

        Activity similarA = activity("相似用户活动A", lecture, 0, 0, now.plusDays(2));
        Activity similarB = activity("相似用户活动B", lecture, 0, 0, now.plusDays(3));

        User neighborA = otherUser("neighbor-a-");
        behavior(neighborA.getId(), historyX.getId(), "VIEW", 0);
        behavior(neighborA.getId(), historyY.getId(), "VIEW", 0);
        registration(neighborA.getId(), similarA.getId());

        User neighborB = otherUser("neighbor-b-");
        behavior(neighborB.getId(), historyZ.getId(), "VIEW", 0);
        registration(neighborB.getId(), similarB.getId());

        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", is(2)))
                .andExpect(jsonPath("$.data[0].activity.title", is("相似用户活动A")))
                .andExpect(jsonPath("$.data[0].reason", is("和你兴趣相似的用户也关注了")))
                .andExpect(jsonPath("$.data[1].activity.title", is("相似用户活动B")));

        org.junit.jupiter.api.Assertions.assertEquals("COLLABORATIVE_CONTENT",
                recommendationRepository.findByUserIdOrderByScoreDesc(user.getId()).get(0).getAlgorithm());
    }

    @Test
    void durationBehaviorContributesToCollaborativeScore() throws Exception {
        Tag lecture = tag("讲座");
        userTagRepository.save(new UserTag(user.getId(), lecture.getId()));

        Activity history = activity("历史停留", lecture, 0, 0, now.minusDays(2));
        behavior(user.getId(), history.getId(), "DURATION", 600);

        Activity similar = activity("停留相似活动", lecture, 0, 0, now.plusDays(2));
        activity("普通活动", lecture, 0, 0, now.plusDays(3));
        User neighbor = otherUser("duration-neighbor-");
        behavior(neighbor.getId(), history.getId(), "VIEW", 0);
        registration(neighbor.getId(), similar.getId());

        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].activity.title", is("停留相似活动")))
                .andExpect(jsonPath("$.data[0].reason", is("和你兴趣相似的用户也关注了")));

        org.junit.jupiter.api.Assertions.assertEquals("COLLABORATIVE_CONTENT",
                recommendationRepository.findByUserIdOrderByScoreDesc(user.getId()).get(0).getAlgorithm());
    }

    @Test
    void shareBehaviorIsUsedInCollaborativeScoring() throws Exception {
        Tag lecture = tag("讲座");
        userTagRepository.save(new UserTag(user.getId(), lecture.getId()));

        Activity history = activity("历史分享", lecture, 0, 0, now.minusDays(2));
        behavior(user.getId(), history.getId(), "SHARE", 0);

        // B 开始更早；如果分享行为没有进入偏好，B 会排在 A 前面。
        Activity similarB = activity("分享相似活动B", lecture, 0, 0, now.plusDays(1));
        Activity similarA = activity("分享相似活动A", lecture, 0, 0, now.plusDays(2));
        User neighbor = otherUser("share-neighbor-");
        behavior(neighbor.getId(), history.getId(), "VIEW", 0);
        registration(neighbor.getId(), similarA.getId());

        mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].activity.title", is("分享相似活动A")))
                .andExpect(jsonPath("$.data[0].reason", is("和你兴趣相似的用户也关注了")));

        org.junit.jupiter.api.Assertions.assertEquals("COLLABORATIVE_CONTENT",
                recommendationRepository.findByUserIdOrderByScoreDesc(user.getId()).get(0).getAlgorithm());
    }

    private User otherUser(String prefix) {
        User other = new User();
        other.setUsername(prefix + System.nanoTime());
        other.setPasswordHash("x");
        other.setStatus((byte) 1);
        return userRepository.save(other);
    }

    private void behavior(Long userId, Long activityId, String type, int durationSeconds) {
        behaviorRepository.save(new Behavior(userId, activityId, type, durationSeconds, now));
    }

    private void registration(Long userId, Long activityId) {
        Registration registration = new Registration();
        registration.setUserId(userId);
        registration.setActivityId(activityId);
        registration.setStatus("REGISTERED");
        registration.setCreatedAt(now);
        registrationRepository.save(registration);
    }

    private Tag tag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setColor("#4F6EF7");
        tag.setSortOrder(0);
        return tagRepository.save(tag);
    }

    private Activity activity(String title, Tag tag, int signupCount, int viewCount, LocalDateTime startTime) {
        Activity activity = new Activity();
        activity.setTitle(title);
        activity.setDescription(title + "的说明");
        activity.setLocation("大学生活动中心");
        activity.setType(tag == null ? "沙龙" : tag.getName());
        activity.setStartTime(startTime);
        activity.setEndTime(startTime.plusHours(2));
        activity.setSignupDeadline(startTime.minusHours(12));
        activity.setCapacity(50);
        activity.setStatus("PUBLISHED");
        activity.setSignupCount(signupCount);
        activity.setViewCount(viewCount);
        activity.setShareCount(0);
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);
        activity = activityRepository.save(activity);
        if (tag != null) {
            activityTagRepository.save(new ActivityTag(activity.getId(), tag.getId()));
        }
        return activity;
    }
}
