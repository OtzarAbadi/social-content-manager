package com.otzar.sscm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.otzar.sscm.entities.Client;
import com.otzar.sscm.entities.Comment;
import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.repository.ClientRepository;
import com.otzar.sscm.repository.CommentRepository;
import com.otzar.sscm.repository.ContentRepository;
import com.otzar.sscm.repository.UserRepository;
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
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ContentRepository contentRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private UserRepository userRepository;

    private Cookie adminCookie;
    private Cookie clientCookie;
    private Cookie otherClientCookie;
    private Client firstClient;
    private Client secondClient;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM content_versions");
        jdbcTemplate.update("DELETE FROM contents");
        jdbcTemplate.update("DELETE FROM clients");
        jdbcTemplate.update("DELETE FROM users WHERE username LIKE 'analytics-%'");

        firstClient = createClient(2L, "Analytics First Client");
        secondClient = createClient(3L, "Analytics Second Client");
        adminCookie = tokenCookie(loginToken("admin", "123456"));
        clientCookie = tokenCookie(loginToken("client1", "123456"));
        otherClientCookie = tokenCookie(loginToken("client2", "123456"));
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/analytics/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminReceivesCorrectGlobalMetricsAndClientSummaries() throws Exception {
        Content draft = createContent(firstClient, "IMAGE", ContentStatus.DRAFT, null);
        Content waiting = createContent(firstClient, "VIDEO", ContentStatus.WAITING_APPROVAL,
                LocalDateTime.of(2026, 8, 10, 10, 0));
        Content published = createContent(firstClient, "TEXT", ContentStatus.PUBLISHED,
                LocalDateTime.of(2026, 6, 2, 12, 0));
        createContent(secondClient, "IMAGE", ContentStatus.APPROVED,
                LocalDateTime.of(2026, 8, 20, 9, 0));
        Content rejected = createContent(secondClient, "VIDEO", ContentStatus.REJECTED, null);
        createComment(draft, 1L);
        createComment(waiting, 2L);
        createComment(rejected, 3L);

        String body = mockMvc.perform(get("/analytics/dashboard").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeClientId").doesNotExist())
                .andExpect(jsonPath("$.totalContents").value(5))
                .andExpect(jsonPath("$.scheduledContents").value(3))
                .andExpect(jsonPath("$.waitingApprovalContents").value(1))
                .andExpect(jsonPath("$.publishedContents").value(1))
                .andExpect(jsonPath("$.totalComments").value(3))
                .andExpect(jsonPath("$.averageCommentsPerContent").value(0.6))
                .andExpect(jsonPath("$.contentsByStatus.DRAFT").value(1))
                .andExpect(jsonPath("$.contentsByStatus.WAITING_APPROVAL").value(1))
                .andExpect(jsonPath("$.contentsByStatus.APPROVED").value(1))
                .andExpect(jsonPath("$.contentsByStatus.REJECTED").value(1))
                .andExpect(jsonPath("$.contentsByStatus.PUBLISHED").value(1))
                .andExpect(jsonPath("$.contentsByType.IMAGE").value(2))
                .andExpect(jsonPath("$.contentsByType.VIDEO").value(2))
                .andExpect(jsonPath("$.contentsByType.TEXT").value(1))
                .andExpect(jsonPath("$.scheduledByMonth", hasSize(2)))
                .andExpect(jsonPath("$.scheduledByMonth[0].month").value("2026-06"))
                .andExpect(jsonPath("$.scheduledByMonth[0].count").value(1))
                .andExpect(jsonPath("$.scheduledByMonth[1].month").value("2026-08"))
                .andExpect(jsonPath("$.scheduledByMonth[1].count").value(2))
                .andExpect(jsonPath("$.clientSummaries", hasSize(2)))
                .andReturn().getResponse().getContentAsString();

        JsonNode firstSummary = findClientSummary(objectMapper.readTree(body), firstClient.getClient_id());
        assertEquals("Analytics First Client", firstSummary.get("businessName").asText());
        assertEquals(3, firstSummary.get("totalContents").asInt());
        assertEquals(2, firstSummary.get("scheduledContents").asInt());
        assertEquals(1, firstSummary.get("waitingApprovalContents").asInt());
        assertEquals(1, firstSummary.get("publishedContents").asInt());
        assertEquals(2, firstSummary.get("commentCount").asInt());
    }

    @Test
    void clientReceivesOnlyOwnedAnalyticsAndNoClientSummaries() throws Exception {
        Content owned = createContent(firstClient, "IMAGE", ContentStatus.WAITING_APPROVAL,
                LocalDateTime.of(2026, 7, 1, 10, 0));
        createComment(owned, 2L);
        createContent(secondClient, "VIDEO", ContentStatus.PUBLISHED,
                LocalDateTime.of(2026, 9, 1, 10, 0));

        mockMvc.perform(get("/analytics/dashboard").cookie(clientCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeClientId").value(firstClient.getClient_id()))
                .andExpect(jsonPath("$.totalContents").value(1))
                .andExpect(jsonPath("$.scheduledContents").value(1))
                .andExpect(jsonPath("$.waitingApprovalContents").value(1))
                .andExpect(jsonPath("$.publishedContents").value(0))
                .andExpect(jsonPath("$.totalComments").value(1))
                .andExpect(jsonPath("$.clientSummaries", empty()));
    }

    @Test
    void emptyClientAnalyticsReturnsZerosAndCompleteZeroMaps() throws Exception {
        mockMvc.perform(get("/analytics/dashboard").cookie(otherClientCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeClientId").value(secondClient.getClient_id()))
                .andExpect(jsonPath("$.totalContents").value(0))
                .andExpect(jsonPath("$.scheduledContents").value(0))
                .andExpect(jsonPath("$.waitingApprovalContents").value(0))
                .andExpect(jsonPath("$.publishedContents").value(0))
                .andExpect(jsonPath("$.totalComments").value(0))
                .andExpect(jsonPath("$.averageCommentsPerContent").value(0.0))
                .andExpect(jsonPath("$.contentsByStatus.DRAFT").value(0))
                .andExpect(jsonPath("$.contentsByStatus.WAITING_APPROVAL").value(0))
                .andExpect(jsonPath("$.contentsByStatus.APPROVED").value(0))
                .andExpect(jsonPath("$.contentsByStatus.REJECTED").value(0))
                .andExpect(jsonPath("$.contentsByStatus.PUBLISHED").value(0))
                .andExpect(jsonPath("$.contentsByType.IMAGE").value(0))
                .andExpect(jsonPath("$.contentsByType.VIDEO").value(0))
                .andExpect(jsonPath("$.contentsByType.TEXT").value(0))
                .andExpect(jsonPath("$.scheduledByMonth", empty()))
                .andExpect(jsonPath("$.clientSummaries", empty()));
    }

    @Test
    void unknownRoleIsForbidden() throws Exception {
        User unknown = new User();
        unknown.setFull_name("Unknown Analytics Role");
        unknown.setEmail("analytics-unknown@example.com");
        unknown.setUsername("analytics-unknown");
        unknown.setPassword("not-used");
        unknown.setRole("UNKNOWN");
        unknown.setToken("analytics-unknown-token");
        userRepository.save(unknown);

        mockMvc.perform(get("/analytics/dashboard")
                        .cookie(tokenCookie("analytics-unknown-token")))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientWithoutAssociatedClientIsForbidden() throws Exception {
        User clientWithoutRecord = new User();
        clientWithoutRecord.setFull_name("Analytics Client Without Record");
        clientWithoutRecord.setEmail("analytics-no-client@example.com");
        clientWithoutRecord.setUsername("analytics-no-client");
        clientWithoutRecord.setPassword("not-used");
        clientWithoutRecord.setRole("CLIENT");
        clientWithoutRecord.setToken("analytics-no-client-token");
        userRepository.save(clientWithoutRecord);

        mockMvc.perform(get("/analytics/dashboard")
                        .cookie(tokenCookie("analytics-no-client-token")))
                .andExpect(status().isForbidden());
    }

    private Client createClient(Long userId, String name) {
        Client client = new Client();
        client.setUser_id(userId);
        client.setBusiness_name(name);
        return clientRepository.save(client);
    }

    private Content createContent(Client client, String type, ContentStatus status,
                                  LocalDateTime plannedPublishDate) {
        Content content = new Content();
        content.setClientId(client.getClient_id());
        content.setTitle("Analytics " + type + " " + status);
        content.setContent_type(type);
        content.setStatus(status);
        content.setPlannedPublishDate(plannedPublishDate);
        return contentRepository.save(content);
    }

    private void createComment(Content content, Long userId) {
        Comment comment = new Comment();
        comment.setContentId(content.getContent_id());
        comment.setUserId(userId);
        comment.setCommentText("Analytics comment");
        commentRepository.save(comment);
    }

    private JsonNode findClientSummary(JsonNode response, Long clientId) {
        for (JsonNode summary : response.get("clientSummaries")) {
            if (summary.get("clientId").asLong() == clientId) return summary;
        }
        throw new AssertionError("Missing client summary for " + clientId);
    }

    private String loginToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private Cookie tokenCookie(String token) {
        return new Cookie("token", token);
    }
}
