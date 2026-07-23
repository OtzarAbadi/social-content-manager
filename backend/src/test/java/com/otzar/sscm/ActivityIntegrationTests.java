package com.otzar.sscm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.otzar.sscm.entities.Client;
import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.entities.ContentVersionChangeType;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.repository.ClientRepository;
import com.otzar.sscm.repository.ContentRepository;
import com.otzar.sscm.repository.UserRepository;
import com.otzar.sscm.service.ContentVersionService;
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

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActivityIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ContentRepository contentRepository;
    @Autowired private ContentVersionService contentVersionService;
    @Autowired private UserRepository userRepository;

    private Cookie adminCookie;
    private Cookie clientCookie;
    private Cookie otherClientCookie;
    private Client firstClient;
    private Client secondClient;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM content_versions");
        jdbcTemplate.update("DELETE FROM contents");
        jdbcTemplate.update("DELETE FROM users WHERE username = 'activity-unknown'");

        firstClient = clientRepository.findByUserId(2L).orElseGet(() -> createClient(2L, "Activity First"));
        secondClient = clientRepository.findByUserId(3L).orElseGet(() -> createClient(3L, "Activity Second"));
        adminCookie = tokenCookie(loginToken("admin", "123456"));
        clientCookie = tokenCookie(loginToken("client1", "123456"));
        otherClientCookie = tokenCookie(loginToken("client2", "123456"));
    }

    @Test
    void adminCanSeeAllActivitiesAndClientNames() throws Exception {
        createVersionedContent(firstClient, "Admin first", ContentVersionChangeType.CREATED, ContentStatus.DRAFT);
        createVersionedContent(secondClient, "Admin second", ContentVersionChangeType.CREATED, ContentStatus.DRAFT);

        mockMvc.perform(get("/activity").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].clientName", everyItem(org.hamcrest.Matchers.notNullValue())));
    }

    @Test
    void clientSeesOnlyActivitiesForOwningClient() throws Exception {
        Content own = createVersionedContent(firstClient, "Own activity", ContentVersionChangeType.CREATED, ContentStatus.DRAFT);
        createVersionedContent(secondClient, "Other activity", ContentVersionChangeType.CREATED, ContentStatus.DRAFT);

        mockMvc.perform(get("/activity").cookie(clientCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].contentId").value(own.getContent_id()))
                .andExpect(jsonPath("$[0].clientId").value(firstClient.getClient_id()))
                .andExpect(jsonPath("$[0].clientName", nullValue()));

        mockMvc.perform(get("/activity").cookie(otherClientCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].contentTitle").value("Other activity"));
    }

    @Test
    void mapsEverySupportedActivityTypeAndSupportsTypeFilter() throws Exception {
        Content content = createContent(firstClient, "All activity mappings");
        snapshot(content, ContentVersionChangeType.CREATED, ContentStatus.DRAFT);
        snapshot(content, ContentVersionChangeType.EDITED, ContentStatus.DRAFT);
        content.setPlannedPublishDate(LocalDateTime.of(2026, 8, 10, 12, 0));
        snapshot(content, ContentVersionChangeType.SCHEDULED, ContentStatus.DRAFT);
        snapshot(content, ContentVersionChangeType.STATUS_CHANGED, ContentStatus.WAITING_APPROVAL);
        snapshot(content, ContentVersionChangeType.STATUS_CHANGED, ContentStatus.APPROVED);
        snapshot(content, ContentVersionChangeType.STATUS_CHANGED, ContentStatus.REJECTED);
        snapshot(content, ContentVersionChangeType.STATUS_CHANGED, ContentStatus.PUBLISHED);

        JsonNode response = objectMapper.readTree(mockMvc.perform(get("/activity").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andReturn().getResponse().getContentAsString());

        assertContainsType(response, "CONTENT_CREATED");
        assertContainsType(response, "CONTENT_UPDATED");
        assertContainsType(response, "SCHEDULED");
        assertContainsType(response, "SENT_FOR_APPROVAL");
        assertContainsType(response, "APPROVED");
        assertContainsType(response, "REJECTED");
        assertContainsType(response, "PUBLISHED");

        mockMvc.perform(get("/activity").param("type", "APPROVED").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("APPROVED"));
    }

    @Test
    void returnsNewestFirstAndHonorsLimit() throws Exception {
        Content content = createContent(firstClient, "Ordered activity");
        snapshot(content, ContentVersionChangeType.CREATED, ContentStatus.DRAFT);
        snapshot(content, ContentVersionChangeType.EDITED, ContentStatus.DRAFT);
        snapshot(content, ContentVersionChangeType.SCHEDULED, ContentStatus.DRAFT);

        mockMvc.perform(get("/activity").param("limit", "2").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].versionNumber").value(3))
                .andExpect(jsonPath("$[1].versionNumber").value(2));
    }

    @Test
    void unknownRoleIsForbidden() throws Exception {
        User unknown = new User();
        unknown.setFull_name("Activity Unknown");
        unknown.setEmail("activity-unknown@example.com");
        unknown.setUsername("activity-unknown");
        unknown.setPassword("not-used");
        unknown.setRole("UNKNOWN");
        unknown.setToken("activity-unknown-token");
        userRepository.save(unknown);

        mockMvc.perform(get("/activity").cookie(tokenCookie("activity-unknown-token")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/activity"))
                .andExpect(status().isUnauthorized());
    }

    private Content createVersionedContent(Client client, String title,
                                           ContentVersionChangeType changeType, ContentStatus status) {
        Content content = createContent(client, title);
        snapshot(content, changeType, status);
        return content;
    }

    private Content createContent(Client client, String title) {
        Content content = new Content();
        content.setClientId(client.getClient_id());
        content.setTitle(title);
        content.setDescription("Activity test");
        content.setContent_type("TEXT");
        content.setStatus(ContentStatus.DRAFT);
        return contentRepository.save(content);
    }

    private void snapshot(Content content, ContentVersionChangeType changeType, ContentStatus status) {
        content.setStatus(status);
        contentRepository.save(content);
        contentVersionService.createSnapshot(content, 1L, changeType);
    }

    private Client createClient(Long userId, String name) {
        Client client = new Client();
        client.setUser_id(userId);
        client.setAdmin_id(1L);
        client.setBusiness_name(name);
        return clientRepository.save(client);
    }

    private void assertContainsType(JsonNode response, String type) {
        boolean found = false;
        for (JsonNode item : response) {
            if (type.equals(item.get("type").asText())) {
                found = true;
                break;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(found, "Missing activity type " + type);
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
