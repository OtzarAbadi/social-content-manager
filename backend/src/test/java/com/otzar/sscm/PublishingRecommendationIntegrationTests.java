package com.otzar.sscm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.repository.ContentRepository;
import com.otzar.sscm.repository.ContentVersionRepository;
import com.otzar.sscm.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublishingRecommendationIntegrationTests {
    private static final String USER_PREFIX = "publishing-recommendation-";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private ContentRepository contentRepository;
    @Autowired private ContentVersionRepository contentVersionRepository;

    private Cookie adminCookie;
    private Cookie clientCookie;
    private Long trackedContentId;

    @BeforeEach
    void setUp() throws Exception {
        cleanUsers();
        adminCookie = tokenCookie(loginToken("admin", "123456"));
        clientCookie = tokenCookie(loginToken("client1", "123456"));
    }

    @AfterEach
    void cleanUp() {
        if (trackedContentId != null) {
            jdbcTemplate.update("DELETE FROM content_versions WHERE content_id = ?", trackedContentId);
            jdbcTemplate.update("DELETE FROM contents WHERE content_id = ?", trackedContentId);
        }
        cleanUsers();
    }

    @Test
    void adminReceivesLocalRulesResponse() throws Exception {
        perform(validRequest(), adminCookie)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPlannedPublishDate").isNotEmpty())
                .andExpect(jsonPath("$.timezone").value("Asia/Jerusalem"))
                .andExpect(jsonPath("$.provider").value("LOCAL_RULES"))
                .andExpect(jsonPath("$.generated").value(true))
                .andExpect(jsonPath("$.ruleVersion").value("v1"))
                .andExpect(jsonPath("$.rationale").isNotEmpty())
                .andExpect(jsonPath("$.inputsUsed[0]").value("CONTENT_TYPE"));
    }

    @Test
    void clientAndNonExactRolesAreForbiddenAndMissingTokenIsUnauthorized() throws Exception {
        perform(validRequest(), clientCookie).andExpect(status().isForbidden());
        perform(validRequest(), null).andExpect(status().isUnauthorized());

        for (String role : new String[]{"UNKNOWN", "admin"}) {
            String token = USER_PREFIX + role;
            createUser(token, role);
            perform(validRequest(), tokenCookie(token)).andExpect(status().isForbidden());
        }
    }

    @Test
    void invalidContentTypeClientAndKeywordsReturnStructuredBadRequest() throws Exception {
        Map<String, Object> invalidType = validRequest();
        invalidType.put("contentType", "AUDIO");
        perform(invalidType, adminCookie)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"));

        Map<String, Object> invalidClient = validRequest();
        invalidClient.put("clientId", 0);
        perform(invalidClient, adminCookie)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.clientId").exists());

        Map<String, Object> tooManyKeywords = validRequest();
        tooManyKeywords.put("keywords", Arrays.asList("א", "ב", "ג", "ד", "ה", "ו"));
        perform(tooManyKeywords, adminCookie)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.keywords").exists());

        Map<String, Object> oversizedKeyword = validRequest();
        oversizedKeyword.put("keywords", Arrays.asList("א".repeat(31)));
        perform(oversizedKeyword, adminCookie).andExpect(status().isBadRequest());
    }

    @Test
    void blankOrOversizedTitleAndMalformedDateReturnBadRequest() throws Exception {
        Map<String, Object> blankTitle = validRequest();
        blankTitle.put("title", "   ");
        perform(blankTitle, adminCookie)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());

        Map<String, Object> oversizedTitle = validRequest();
        oversizedTitle.put("title", "א".repeat(151));
        perform(oversizedTitle, adminCookie).andExpect(status().isBadRequest());

        Map<String, Object> malformedDate = validRequest();
        malformedDate.put("existingPlannedPublishDate", "tomorrow");
        perform(malformedDate, adminCookie)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void endpointLeavesDatabaseContentAndVersionsUnchanged() throws Exception {
        Content content = new Content();
        content.setClientId(1L);
        content.setTitle("Recommendation write guard");
        content.setContent_type("IMAGE");
        content.setStatus(ContentStatus.DRAFT);
        content.setPlannedPublishDate(LocalDateTime.of(2026, 9, 10, 11, 0));
        trackedContentId = contentRepository.save(content).getContent_id();

        Map<String, Integer> before = tableCounts();
        int versionsBefore = contentVersionRepository.findByContentIdOrdered(trackedContentId).size();
        perform(validRequest(), adminCookie).andExpect(status().isOk());

        assertEquals(before, tableCounts());
        assertEquals(LocalDateTime.of(2026, 9, 10, 11, 0),
                contentRepository.findById(trackedContentId).orElseThrow().getPlannedPublishDate());
        assertEquals(versionsBefore, contentVersionRepository.findByContentIdOrdered(trackedContentId).size());
    }

    private ResultActions perform(Map<String, Object> request, Cookie cookie) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder =
                post("/contents/publishing-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request));
        if (cookie != null) builder.cookie(cookie);
        return mockMvc.perform(builder);
    }

    private Map<String, Object> validRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("contentType", "IMAGE");
        request.put("title", "מדריך קיץ");
        request.put("keywords", Arrays.asList("קיץ", "טיפים"));
        request.put("clientId", 1);
        request.put("existingPlannedPublishDate", "2026-08-01T10:00:00");
        return request;
    }

    private Map<String, Integer> tableCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String table : Arrays.asList("users", "clients", "contents", "content_versions",
                "comments", "notifications")) {
            counts.put(table, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class));
        }
        return counts;
    }

    private void createUser(String token, String role) {
        User user = new User();
        user.setFull_name("Publishing Recommendation Test");
        user.setEmail(token + "@example.com");
        user.setUsername(token);
        user.setPassword("not-used");
        user.setRole(role);
        user.setToken(token);
        userRepository.save(user);
    }

    private void cleanUsers() {
        jdbcTemplate.update("DELETE FROM users WHERE username LIKE ?", USER_PREFIX + "%");
    }

    private String loginToken(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private Cookie tokenCookie(String token) { return new Cookie("token", token); }
}
