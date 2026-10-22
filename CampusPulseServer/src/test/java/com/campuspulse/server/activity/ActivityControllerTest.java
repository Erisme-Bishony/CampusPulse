package com.campuspulse.server.activity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.campuspulse.server.domain.Activity;
import com.campuspulse.server.domain.ActivityTag;
import com.campuspulse.server.domain.Tag;
import com.campuspulse.server.repository.ActivityRepository;
import com.campuspulse.server.repository.ActivityTagRepository;
import com.campuspulse.server.repository.TagRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityTagRepository activityTagRepository;

    @Autowired
    private TagRepository tagRepository;

    private Tag lectureTag;
    private Tag sportsTag;
    private Tag artTag;
    private Activity lecture;
    private Activity basketball;
    private Activity concert;

    @BeforeEach
    void setUp() {
        activityTagRepository.deleteAll();
        activityRepository.deleteAll();
        tagRepository.deleteAll();

        lectureTag = saveTag("讲座", "#4A90D9", 1);
        sportsTag = saveTag("体育", "#E85D4A", 2);
        artTag = saveTag("文艺", "#E67E22", 3);

        lecture = saveActivity("人工智能前沿讲座", "讲座", "图书馆报告厅",
                LocalDate.now().plusDays(2).atTime(19, 0), 100, 36, "PUBLISHED", lectureTag);
        basketball = saveActivity("校园篮球赛", "比赛", "东区篮球场",
                LocalDate.now().plusDays(3).atTime(15, 0), 20, 20, "PUBLISHED", sportsTag);
        saveActivity("草稿活动", "讲座", "未发布地点",
                LocalDate.now().plusDays(4).atTime(10, 0), 50, 0, "DRAFT", lectureTag);
        concert = saveActivity("秋季音乐会", "演出", "大学生活动中心",
                LocalDate.now().plusDays(5).atTime(19, 30), 80, 10, "PUBLISHED", artTag);
    }

    @Test
    void listsTagsInDisplayOrder() throws Exception {
        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("讲座"))
                .andExpect(jsonPath("$.data[0].color").value("#4A90D9"))
                .andExpect(jsonPath("$.data[2].name").value("文艺"));
    }

    @Test
    void listsOnlyPublishedActivitiesWithPagination() throws Exception {
        mockMvc.perform(get("/api/v1/activities").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].title").value("人工智能前沿讲座"))
                .andExpect(jsonPath("$.data.items[0].remaining").value(64))
                .andExpect(jsonPath("$.data.items[0].tags[0].name").value("讲座"));

        mockMvc.perform(get("/api/v1/activities").param("page", "2").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("秋季音乐会"));
    }

    @Test
    void filtersByKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/activities").param("keyword", "讲座"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("人工智能前沿讲座"));

        mockMvc.perform(get("/api/v1/activities").param("keyword", "篮球"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("校园篮球赛"));
    }

    @Test
    void filtersByTagAndTypeAndDate() throws Exception {
        mockMvc.perform(get("/api/v1/activities").param("tagId", artTag.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("秋季音乐会"));

        mockMvc.perform(get("/api/v1/activities").param("type", "比赛"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("校园篮球赛"));

        mockMvc.perform(get("/api/v1/activities").param("date", concert.getStartTime().toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("秋季音乐会"));
    }

    @Test
    void returnsActivityDetailWithTags() throws Exception {
        mockMvc.perform(get("/api/v1/activities/" + lecture.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("人工智能前沿讲座"))
                .andExpect(jsonPath("$.data.description").value("人工智能前沿讲座 的详细介绍"))
                .andExpect(jsonPath("$.data.organizer").value("学生会"))
                .andExpect(jsonPath("$.data.viewCount").value(10))
                .andExpect(jsonPath("$.data.tags[0].name").value("讲座"))
                .andExpect(jsonPath("$.data.remaining").value(64));
    }

    @Test
    void hidesDraftAndMissingActivities() throws Exception {
        Activity draft = activityRepository.findAll().stream()
                .filter(activity -> "DRAFT".equals(activity.getStatus()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/v1/activities/" + draft.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        mockMvc.perform(get("/api/v1/activities/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void rejectsTooShortKeywordAndInvalidPage() throws Exception {
        mockMvc.perform(get("/api/v1/activities").param("keyword", "讲"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));

        mockMvc.perform(get("/api/v1/activities").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    private Tag saveTag(String name, String color, int sortOrder) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setColor(color);
        tag.setSortOrder(sortOrder);
        return tagRepository.save(tag);
    }

    private Activity saveActivity(String title,
                                  String type,
                                  String location,
                                  LocalDateTime startTime,
                                  int capacity,
                                  int signupCount,
                                  String status,
                                  Tag... tags) {
        LocalDateTime now = LocalDateTime.now();
        Activity activity = new Activity();
        activity.setTitle(title);
        activity.setDescription(title + " 的详细介绍");
        activity.setLocation(location);
        activity.setType(type);
        activity.setStartTime(startTime);
        activity.setEndTime(startTime.plusHours(2));
        activity.setSignupDeadline(startTime.minusDays(1));
        activity.setCapacity(capacity);
        activity.setSignupCount(signupCount);
        activity.setStatus(status);
        activity.setOrganizer("学生会");
        activity.setViewCount(10);
        activity.setShareCount(2);
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);
        Activity saved = activityRepository.save(activity);

        List<ActivityTag> links = java.util.Arrays.stream(tags)
                .map(tag -> new ActivityTag(saved.getId(), tag.getId()))
                .toList();
        activityTagRepository.saveAll(links);
        return saved;
    }
}
