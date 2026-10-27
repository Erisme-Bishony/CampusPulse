package com.campuspulse.server.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.server.domain.Admin;
import com.campuspulse.server.domain.Tag;
import com.campuspulse.server.repository.ActivityRepository;
import com.campuspulse.server.repository.AdminRepository;
import com.campuspulse.server.repository.TagRepository;

import org.springframework.mock.web.MockHttpSession;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (adminRepository.findByUsername("admin").isEmpty()) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setDisplayName("系统管理员");
            admin.setStatus((byte) 1);
            adminRepository.save(admin);
        }
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CampusPulse 管理后台")));
    }

    @Test
    void adminPageRequiresLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    void loginSetsSessionAndDashboardRenders() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("仪表盘")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("活动总数")));
        assertThat(session.getAttribute(AdminSession.ADMIN_ID)).isNotNull();
    }

    @Test
    void wrongPasswordShowsError() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .param("username", "admin")
                        .param("password", "wrong-pass"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("用户名或密码错误")));
    }

    @Test
    void createActivityWithCsrf() throws Exception {
        MockHttpSession session = login();
        String csrf = (String) session.getAttribute(AdminSession.CSRF_TOKEN);

        mockMvc.perform(post("/admin/activities/save")
                        .session(session)
                        .param("_csrf", csrf)
                        .param("title", "后台创建的活动")
                        .param("description", "由管理后台创建")
                        .param("location", "教学楼 A101")
                        .param("type", "讲座")
                        .param("startTime", "2026-11-20T19:00")
                        .param("endTime", "2026-11-20T21:00")
                        .param("signupDeadline", "2026-11-19T18:00")
                        .param("capacity", "30")
                        .param("status", "PUBLISHED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/activities"));

        assertThat(activityRepository.findAll())
                .anyMatch(activity -> "后台创建的活动".equals(activity.getTitle()));
    }

    @Test
    void postWithoutCsrfIsForbidden() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post("/admin/activities/999/toggle").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTag() throws Exception {
        MockHttpSession session = login();
        String csrf = (String) session.getAttribute(AdminSession.CSRF_TOKEN);

        mockMvc.perform(post("/admin/tags/create")
                        .session(session)
                        .param("_csrf", csrf)
                        .param("name", "测试标签")
                        .param("color", "#123456")
                        .param("sortOrder", "9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"));

        Tag tag = tagRepository.findByName("测试标签").orElseThrow();
        assertThat(tag.getColor()).isEqualTo("#123456");
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/login")
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
