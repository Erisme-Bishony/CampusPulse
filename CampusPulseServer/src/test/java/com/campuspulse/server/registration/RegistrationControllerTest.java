package com.campuspulse.server.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.campuspulse.server.common.BizException;
import com.campuspulse.server.common.JwtUtil;
import com.campuspulse.server.domain.Activity;
import com.campuspulse.server.domain.Registration;
import com.campuspulse.server.repository.ActivityRepository;
import com.campuspulse.server.repository.ActivityTagRepository;
import com.campuspulse.server.repository.RegistrationRepository;

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationControllerTest {

    private static final long USER_ID = 101L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityTagRepository activityTagRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RegistrationService registrationService;

    private Activity openActivity;
    private String bearer;

    @BeforeEach
    void setUp() {
        registrationRepository.deleteAll();
        activityTagRepository.deleteAll();
        activityRepository.deleteAll();

        openActivity = saveActivity("开放报名的活动", 10, 0,
                LocalDateTime.now().plusDays(2), "PUBLISHED");
        bearer = "Bearer " + jwtUtil.generate(USER_ID, "USER");
    }

    @Test
    void registersAndReturnsStatus() throws Exception {
        mockMvc.perform(post("/api/v1/activities/{id}/registrations", openActivity.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.activityId").value(openActivity.getId()))
                .andExpect(jsonPath("$.data.status").value("REGISTERED"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/activities/{id}/registration-status", openActivity.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registered").value(true))
                .andExpect(jsonPath("$.data.status").value("REGISTERED"));

        Activity saved = activityRepository.findById(openActivity.getId()).orElseThrow();
        assertThat(saved.getSignupCount()).isEqualTo(1);
        assertThat(registrationRepository.findByUserIdAndActivityId(USER_ID, openActivity.getId()))
                .isPresent();
    }

    @Test
    void rejectsDuplicateRegistration() throws Exception {
        register(openActivity.getId());

        mockMvc.perform(post("/api/v1/activities/{id}/registrations", openActivity.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40902));

        Activity saved = activityRepository.findById(openActivity.getId()).orElseThrow();
        assertThat(saved.getSignupCount()).isEqualTo(1);
        assertThat(registrationRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsFullActivity() throws Exception {
        Activity full = saveActivity("满额活动", 5, 5,
                LocalDateTime.now().plusDays(2), "PUBLISHED");

        mockMvc.perform(post("/api/v1/activities/{id}/registrations", full.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40903));

        assertThat(activityRepository.findById(full.getId()).orElseThrow().getSignupCount()).isEqualTo(5);
        assertThat(registrationRepository.count()).isZero();
    }

    @Test
    void rejectsClosedRegistration() throws Exception {
        Activity closed = saveActivity("已截止活动", 10, 0,
                LocalDateTime.now().minusHours(1), "PUBLISHED");

        mockMvc.perform(post("/api/v1/activities/{id}/registrations", closed.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40904));

        assertThat(registrationRepository.count()).isZero();
    }

    @Test
    void cancelsAndAllowsRegisteringAgain() throws Exception {
        register(openActivity.getId());

        mockMvc.perform(delete("/api/v1/activities/{id}/registrations", openActivity.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(activityRepository.findById(openActivity.getId()).orElseThrow().getSignupCount()).isZero();
        mockMvc.perform(get("/api/v1/activities/{id}/registration-status", openActivity.getId())
                        .header("Authorization", bearer))
                .andExpect(jsonPath("$.data.registered").value(false))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(delete("/api/v1/activities/{id}/registrations", openActivity.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk());

        register(openActivity.getId());
        assertThat(activityRepository.findById(openActivity.getId()).orElseThrow().getSignupCount()).isEqualTo(1);
        assertThat(registrationRepository.count()).isEqualTo(1);
        Registration registration = registrationRepository
                .findByUserIdAndActivityId(USER_ID, openActivity.getId())
                .orElseThrow();
        assertThat(registration.getStatus()).isEqualTo("REGISTERED");
    }

    @Test
    void requiresToken() throws Exception {
        mockMvc.perform(post("/api/v1/activities/{id}/registrations", openActivity.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));

        mockMvc.perform(get("/api/v1/activities/{id}/registration-status", openActivity.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void listsMyRegistrationsWithActivityBrief() throws Exception {
        register(openActivity.getId());

        mockMvc.perform(get("/api/v1/users/me/registrations")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("REGISTERED"))
                .andExpect(jsonPath("$.data[0].activity.id").value(openActivity.getId()))
                .andExpect(jsonPath("$.data[0].activity.title").value("开放报名的活动"))
                .andExpect(jsonPath("$.data[0].activity.location").value("测试地点"));
    }

    @Test
    void hidesMissingAndDraftActivities() throws Exception {
        Activity draft = saveActivity("草稿活动", 10, 0,
                LocalDateTime.now().plusDays(2), "DRAFT");

        mockMvc.perform(post("/api/v1/activities/{id}/registrations", draft.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        mockMvc.perform(post("/api/v1/activities/999999/registrations")
                        .header("Authorization", bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void allowsOnlyCapacityWhenRegistrationsRace() throws Exception {
        Activity oneSeat = saveActivity("并发抢名额活动", 1, 0,
                LocalDateTime.now().plusDays(2), "PUBLISHED");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<String>> futures = List.of(
                    pool.submit(() -> raceRegister(ready, start, 201L, oneSeat.getId())),
                    pool.submit(() -> raceRegister(ready, start, 202L, oneSeat.getId())));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<String> results = futures.stream()
                    .map(future -> {
                        try {
                            return future.get(10, TimeUnit.SECONDS);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();
            assertThat(results).containsExactlyInAnyOrder("OK", "ACTIVITY_FULL");
            assertThat(activityRepository.findById(oneSeat.getId()).orElseThrow().getSignupCount()).isEqualTo(1);
            assertThat(registrationRepository.count()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private String raceRegister(CountDownLatch ready,
                                CountDownLatch start,
                                Long userId,
                                Long activityId) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            registrationService.register(userId, activityId);
            return "OK";
        } catch (BizException exception) {
            return exception.getErrorCode().name();
        }
    }

    private void register(Long activityId) throws Exception {
        mockMvc.perform(post("/api/v1/activities/{id}/registrations", activityId)
                        .header("Authorization", bearer))
                .andExpect(status().isOk());
    }

    private Activity saveActivity(String title,
                                  int capacity,
                                  int signupCount,
                                  LocalDateTime signupDeadline,
                                  String status) {
        LocalDateTime startTime = LocalDateTime.now().plusDays(3);
        LocalDateTime now = LocalDateTime.now();
        Activity activity = new Activity();
        activity.setTitle(title);
        activity.setDescription(title + " 的详细介绍");
        activity.setLocation("测试地点");
        activity.setType("讲座");
        activity.setStartTime(startTime);
        activity.setEndTime(startTime.plusHours(2));
        activity.setSignupDeadline(signupDeadline);
        activity.setCapacity(capacity);
        activity.setSignupCount(signupCount);
        activity.setStatus(status);
        activity.setOrganizer("学生会");
        activity.setViewCount(0);
        activity.setShareCount(0);
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);
        return activityRepository.save(activity);
    }
}
