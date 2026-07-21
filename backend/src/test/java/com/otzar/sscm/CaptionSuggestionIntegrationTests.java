package com.otzar.sscm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.otzar.sscm.entities.User;
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

import javax.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
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
class CaptionSuggestionIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;

    private Cookie adminCookie;
    private Cookie clientCookie;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM users WHERE username = 'caption-unknown'");
        adminCookie = tokenCookie(loginToken("admin", "123456"));
        clientCookie = tokenCookie(loginToken("client1", "123456"));
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM users WHERE username = 'caption-unknown'");
    }

    @Test
    void adminReceivesDeterministicLocalSuggestionWithSanitizedHashtags() throws Exception {
        Map<String, Object> request = validRequest();
        request.put("tone", "PROMOTIONAL");
        request.put("keywords", Arrays.asList("קיץ חם!", "טיפוח", "טיפוח"));

        String first = performSuggestion(request, adminCookie)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("LOCAL_SIMULATOR"))
                .andExpect(jsonPath("$.generated").value(true))
                .andExpect(jsonPath("$.caption").isNotEmpty())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String second = performSuggestion(request, adminCookie)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertEquals(first, second);
        String caption = objectMapper.readTree(first).get("caption").asText();
        org.junit.jupiter.api.Assertions.assertTrue(caption.contains("#קיץ_חם"));
        org.junit.jupiter.api.Assertions.assertFalse(caption.contains("!"));
    }

    @Test
    void clientAndUnknownRoleAreForbiddenAndUnauthenticatedIsUnauthorized() throws Exception {
        performSuggestion(validRequest(), clientCookie).andExpect(status().isForbidden());
        performSuggestion(validRequest(), null).andExpect(status().isUnauthorized());

        User unknown = new User();
        unknown.setFull_name("Caption Unknown");
        unknown.setEmail("caption-unknown@example.com");
        unknown.setUsername("caption-unknown");
        unknown.setPassword("not-used");
        unknown.setRole("UNKNOWN");
        unknown.setToken("caption-unknown-token");
        userRepository.save(unknown);

        performSuggestion(validRequest(), tokenCookie("caption-unknown-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void blankAndOversizedTitlesReturnStructuredValidationErrors() throws Exception {
        Map<String, Object> blank = validRequest();
        blank.put("title", "   ");
        performSuggestion(blank, adminCookie)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").value("Title is required"));

        Map<String, Object> oversized = validRequest();
        oversized.put("title", "א".repeat(151));
        performSuggestion(oversized, adminCookie)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void tooManyAndOversizedKeywordsReturnBadRequest() throws Exception {
        Map<String, Object> tooMany = validRequest();
        tooMany.put("keywords", Arrays.asList("א", "ב", "ג", "ד", "ה", "ו"));
        performSuggestion(tooMany, adminCookie)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.keywords").exists());

        Map<String, Object> oversized = validRequest();
        oversized.put("keywords", Arrays.asList("א".repeat(31)));
        performSuggestion(oversized, adminCookie)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidEnumsAndUnsupportedLanguageReturnStructuredBadRequest() throws Exception {
        for (Map.Entry<String, String> invalid : Map.of(
                "tone", "CASUAL",
                "contentType", "AUDIO",
                "language", "EN").entrySet()) {
            Map<String, Object> request = validRequest();
            request.put(invalid.getKey(), invalid.getValue());
            performSuggestion(request, adminCookie)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation failed"))
                    .andExpect(jsonPath("$.fieldErrors").isMap());
        }
    }

    @Test
    void endpointDoesNotWriteToApplicationTables() throws Exception {
        Map<String, Integer> before = tableCounts();
        performSuggestion(validRequest(), adminCookie).andExpect(status().isOk());
        assertEquals(before, tableCounts());
    }

    private org.springframework.test.web.servlet.ResultActions performSuggestion(
            Map<String, Object> request, Cookie cookie) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder =
                post("/contents/caption-suggestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request));
        if (cookie != null) builder.cookie(cookie);
        return mockMvc.perform(builder);
    }

    private Map<String, Object> validRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", "טיפים לקיץ");
        request.put("contentType", "IMAGE");
        request.put("tone", "FRIENDLY");
        request.put("keywords", Arrays.asList("קיץ", "טיפוח"));
        request.put("language", "HE");
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

    private String loginToken(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private Cookie tokenCookie(String token) {
        return new Cookie("token", token);
    }
}
