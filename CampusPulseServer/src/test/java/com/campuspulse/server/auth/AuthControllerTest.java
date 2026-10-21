package com.campuspulse.server.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.campuspulse.server.domain.Tag;
import com.campuspulse.server.repository.TagRepository;
import com.campuspulse.server.repository.UserRepository;
import com.campuspulse.server.repository.UserTagRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String REGISTER_BODY = """
            {"username":"alice","password":"123456","confirmPassword":"123456",
             "email":"alice@example.com","phone":"13800000000","tagIds":[]}
            """;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTagRepository userTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void cleanDatabase() {
        userTagRepository.deleteAll();
        userRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    void registersAndReturnsToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("alice"))
                .andExpect(jsonPath("$.data.user.role").value("USER"));
    }

    @Test
    void rejectsDuplicateUsername() throws Exception {
        register();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901));
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        register();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102));
    }

    @Test
    void rejectsMismatchedConfirmPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bob","password":"123456","confirmPassword":"654321","tagIds":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void requiresTokenForMe() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void returnsCurrentUserWithValidToken() throws Exception {
        String token = register();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }

    @Test
    void updatesInterestsAndProfile() throws Exception {
        String token = register();
        Tag tag = new Tag();
        tag.setName("体育");
        tag.setColor("#E85D4A");
        tag.setSortOrder(1);
        tag = tagRepository.save(tag);

        mockMvc.perform(put("/api/v1/users/me/interests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[" + tag.getId() + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tagIds[0]").value(tag.getId()));

        mockMvc.perform(get("/api/v1/users/me/interests")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value(tag.getId()));

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"phone\":\"13900000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("new@example.com"));
    }

    @Test
    void rejectsUnknownInterestTag() throws Exception {
        String token = register();

        mockMvc.perform(put("/api/v1/users/me/interests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[9999]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    private String register() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("token").asText();
    }
}
