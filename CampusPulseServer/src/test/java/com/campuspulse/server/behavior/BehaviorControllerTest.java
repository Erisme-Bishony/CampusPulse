package com.campuspulse.server.behavior;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.campuspulse.server.common.JwtUtil;
import com.campuspulse.server.domain.Activity;
import com.campuspulse.server.domain.User;
import com.campuspulse.server.repository.ActivityRepository;
import com.campuspulse.server.repository.ActivityTagRepository;
import com.campuspulse.server.repository.BehaviorRepository;
import com.campuspulse.server.repository.RegistrationRepository;
import com.campuspulse.server.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class BehaviorControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private ActivityTagRepository activityTagRepository;
    @Autowired
    private RegistrationRepository registrationRepository;
    @Autowired
    private BehaviorRepository behaviorRepository;

    private String token;
    private Activity activity;

    @BeforeEach
    void setUp() {
        behaviorRepository.deleteAll();
        registrationRepository.deleteAll();
        activityTagRepository.deleteAll();
        activityRepository.deleteAll();

        User user = new User();
        user.setUsername("behavior-" + System.nanoTime());
        user.setPasswordHash("x");
        user.setStatus((byte) 1);
        user = userRepository.save(user);
        token = jwtUtil.generate(user.getId(), "USER");

        LocalDateTime now = LocalDateTime.now().withNano(0);
        activity = new Activity();
        activity.setTitle("行为测试活动");
        activity.setDescription("用于行为接口测试");
        activity.setLocation("体育馆");
        activity.setType("讲座");
        activity.setStartTime(now.plusDays(2));
        activity.setEndTime(now.plusDays(2).plusHours(2));
        activity.setSignupDeadline(now.plusDays(1));
        activity.setCapacity(30);
        activity.setStatus("PUBLISHED");
        activity.setViewCount(10);
        activity.setSignupCount(3);
        activity.setShareCount(5);
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);
        activity = activityRepository.save(activity);
    }

    @Test
    void viewIncrementsActivityCounter() throws Exception {
        mockMvc.perform(post("/api/v1/behaviors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activityId": %d, "type": "VIEW", "durationSeconds": 3}
                                """.formatted(activity.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.type", is("VIEW")))
                .andExpect(jsonPath("$.data.activityId", is(activity.getId().intValue())));

        Activity updated = activityRepository.findById(activity.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(11, updated.getViewCount());
    }

    @Test
    void shareIncrementsShareCounterAndDurationDoesNot() throws Exception {
        mockMvc.perform(post("/api/v1/behaviors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activityId": %d, "type": "SHARE"}
                                """.formatted(activity.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type", is("SHARE")));

        mockMvc.perform(post("/api/v1/behaviors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activityId": %d, "type": "DURATION", "durationSeconds": 42}
                                """.formatted(activity.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type", is("DURATION")))
                .andExpect(jsonPath("$.data.durationSeconds", is(42)));

        Activity updated = activityRepository.findById(activity.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(6, updated.getShareCount());
        org.junit.jupiter.api.Assertions.assertEquals(10, updated.getViewCount());
    }

    @Test
    void rejectsInvalidTypeAndMissingActivity() throws Exception {
        mockMvc.perform(post("/api/v1/behaviors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activityId": %d, "type": "CLICK"}
                                """.formatted(activity.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(40001)));

        mockMvc.perform(post("/api/v1/behaviors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activityId": 999999, "type": "VIEW"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(40401)));
    }

    @Test
    void rejectsDurationWithoutPositiveSeconds() throws Exception {
        mockMvc.perform(post("/api/v1/behaviors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activityId": %d, "type": "DURATION", "durationSeconds": 0}
                                """.formatted(activity.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(40001)));
    }

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(post("/api/v1/behaviors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activityId": %d, "type": "VIEW"}
                                """.formatted(activity.getId())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is(40101)));
    }
}
