package com.otzar.sscm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.otzar.sscm.entities.Client;
import com.otzar.sscm.entities.Comment;
import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.repository.ClientRepository;
import com.otzar.sscm.repository.ContentRepository;
import com.otzar.sscm.repository.NotificationRepository;
import com.otzar.sscm.repository.UserRepository;
import com.otzar.sscm.service.FileStorageService;
import com.otzar.sscm.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;

import javax.servlet.http.Cookie;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialContentManagerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileStorageService fileStorageService;

    private Cookie adminCookie;
    private Cookie clientCookie;

    @BeforeEach
    void setUp() throws Exception {
        adminCookie = tokenCookie(loginToken("admin", "123456"));
        clientCookie = tokenCookie(loginToken("client1", "123456"));
    }

    @Test
    void contextLoads() {
    }

    @Test
    void createContentWithUploadedImageBindsAllMultipartFields() throws Exception {
        Client client = createClient(2L);
        MockMultipartFile image = new MockMultipartFile(
                "file", "test-image.png", "image/png", new byte[]{1, 2, 3});

        MvcResult result = mockMvc.perform(multipart("/contents")
                        .file(image)
                        .param("clientId", client.getClient_id().toString())
                        .param("title", "Uploaded image content")
                        .param("description", "Multipart description")
                        .param("contentType", "IMAGE")
                        .param("plannedPublishDate", "2026-07-01T12:30")
                        .cookie(adminCookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(client.getClient_id()))
                .andExpect(jsonPath("$.title").value("Uploaded image content"))
                .andExpect(jsonPath("$.description").value("Multipart description"))
                .andExpect(jsonPath("$.content_type").value("IMAGE"))
                .andExpect(jsonPath("$.file_url").value(org.hamcrest.Matchers.startsWith("/uploads/")))
                .andReturn();

        String fileUrl = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("file_url").asText();
        java.nio.file.Files.deleteIfExists(
                fileStorageService.getUploadDirectory().resolve(fileUrl.substring("/uploads/".length())));
    }

    @Test
    void jsonContentCreationRemainsSupported() throws Exception {
        Client client = createClient(2L);
        Content content = new Content();
        content.setClientId(client.getClient_id());
        content.setTitle("JSON content");
        content.setDescription("Created without a file");
        content.setContent_type("TEXT");

        mockMvc.perform(post("/contents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(content))
                        .cookie(adminCookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("JSON content"));
    }

    @Test
    void unsupportedCreateContentMediaTypeReturnsReadableError() throws Exception {
        mockMvc.perform(post("/contents")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not a supported content request")
                        .cookie(adminCookie))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message",
                        org.hamcrest.Matchers.containsString("text/plain")));
    }

    @Test
    void getContentsByClientReturnsEmptyListForExistingClientWithNoContent() throws Exception {
        Client client = new Client();
        client.setUser_id(2L);
        client.setAdmin_id(null);
        client.setBusiness_name("Client Without Content");
        client.setPhone("0500000000");

        clientRepository.save(client);

        mockMvc.perform(get("/contents/client/{clientId}", client.getClient_id()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void getContentsByClientReturnsNotFoundForMissingClient() throws Exception {
        mockMvc.perform(get("/contents/client/{clientId}", 9999L).cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void sendForApprovalChangesDraftToWaitingApproval() throws Exception {
        Content content = createContent(ContentStatus.DRAFT);

        mockMvc.perform(put("/contents/{id}/send-for-approval", content.getContent_id()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_APPROVAL"));
    }

    @Test
    void approveChangesWaitingApprovalToApproved() throws Exception {
        Content content = createContent(ContentStatus.WAITING_APPROVAL);

        mockMvc.perform(put("/contents/{id}/approve", content.getContent_id()).cookie(clientCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectChangesWaitingApprovalToRejected() throws Exception {
        Content content = createContent(ContentStatus.WAITING_APPROVAL);

        mockMvc.perform(put("/contents/{id}/reject", content.getContent_id()).cookie(clientCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"The message needs revision\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        org.junit.jupiter.api.Assertions.assertEquals("The message needs revision",
                commentRepository.getCommentsByContentId(content.getContent_id()).get(0).getCommentText());
    }

    @Test
    void rejectRequiresReason() throws Exception {
        Content content = createContent(ContentStatus.WAITING_APPROVAL);

        mockMvc.perform(put("/contents/{id}/reject", content.getContent_id()).cookie(clientCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectedContentCanBeSentForApprovalAgain() throws Exception {
        Content content = createContent(ContentStatus.REJECTED);

        mockMvc.perform(put("/contents/{id}/send-for-approval", content.getContent_id()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_APPROVAL"));
    }

    @Test
    void publishChangesApprovedToPublished() throws Exception {
        Content content = createContent(ContentStatus.APPROVED);

        mockMvc.perform(put("/contents/{id}/publish", content.getContent_id()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void publishRejectsContentThatIsNotApproved() throws Exception {
        Content content = createContent(ContentStatus.DRAFT);

        mockMvc.perform(put("/contents/{id}/publish", content.getContent_id()).cookie(adminCookie))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCannotApproveInsteadOfClient() throws Exception {
        Content content = createContent(ContentStatus.WAITING_APPROVAL);

        mockMvc.perform(put("/contents/{id}/approve", content.getContent_id()).cookie(adminCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientCannotPublishContent() throws Exception {
        Content content = createContent(ContentStatus.APPROVED);

        mockMvc.perform(put("/contents/{id}/publish", content.getContent_id()).cookie(clientCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientCannotReadAnotherClientsContent() throws Exception {
        Content content = createContentForUser(3L, ContentStatus.WAITING_APPROVAL);

        mockMvc.perform(get("/contents/{id}", content.getContent_id()).cookie(clientCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUpdateOnlyThePlannedPublishDate() throws Exception {
        Content content = createContent(ContentStatus.DRAFT);

        mockMvc.perform(put("/contents/{id}/schedule", content.getContent_id())
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plannedPublishDate\":\"2026-08-12T15:45:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plannedPublishDate").value("2026-08-12T15:45:00"))
                .andExpect(jsonPath("$.title").value(content.getTitle()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void clientCannotUpdateSchedule() throws Exception {
        Content content = createContent(ContentStatus.DRAFT);
        content.setPlannedPublishDate(LocalDateTime.of(2026, 8, 1, 10, 0));
        contentRepository.save(content);

        mockMvc.perform(put("/contents/{id}/schedule", content.getContent_id())
                        .cookie(clientCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plannedPublishDate\":\"2026-08-12T15:45:00\"}"))
                .andExpect(status().isForbidden());

        org.junit.jupiter.api.Assertions.assertEquals(
                LocalDateTime.of(2026, 8, 1, 10, 0),
                contentRepository.findById(content.getContent_id()).orElseThrow().getPlannedPublishDate());
    }

    @Test
    void clientReceivesNotificationWhenContentIsSentForApproval() throws Exception {
        Content content = createContent(ContentStatus.DRAFT);
        mockMvc.perform(put("/contents/{id}/send-for-approval", content.getContent_id()).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/notifications").cookie(clientCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CONTENT_WAITING_APPROVAL"))
                .andExpect(jsonPath("$[0].relatedContentId").value(content.getContent_id()));
    }

    @Test
    void adminReceivesApprovalAndRejectionNotifications() throws Exception {
        Content approved = createContent(ContentStatus.WAITING_APPROVAL);
        mockMvc.perform(put("/contents/{id}/approve", approved.getContent_id()).cookie(clientCookie))
                .andExpect(status().isOk());

        Content rejected = createContent(ContentStatus.WAITING_APPROVAL);
        mockMvc.perform(put("/contents/{id}/reject", rejected.getContent_id()).cookie(clientCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Please change the caption\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/notifications").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CONTENT_REJECTED"))
                .andExpect(jsonPath("$[0].message", org.hamcrest.Matchers.containsString("Please change the caption")))
                .andExpect(jsonPath("$[1].type").value("CONTENT_APPROVED"));
    }

    @Test
    void commentNotifiesOppositeParty() throws Exception {
        Content content = createContent(ContentStatus.DRAFT);
        mockMvc.perform(post("/comments").cookie(clientCookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentId\":" + content.getContent_id() + ",\"commentText\":\"A client note\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/notifications").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("COMMENT_ADDED"))
                .andExpect(jsonPath("$[0].relatedContentId").value(content.getContent_id()));
    }

    @Test
    void adminDeletesExistingComment() throws Exception {
        Comment comment = createComment(2L);

        mockMvc.perform(delete("/comments/{id}", comment.getCommentId()).cookie(adminCookie))
                .andExpect(status().isNoContent());
    }

    @Test
    void clientDeletesOwnComment() throws Exception {
        Comment comment = createComment(2L);

        mockMvc.perform(delete("/comments/{id}", comment.getCommentId()).cookie(clientCookie))
                .andExpect(status().isNoContent());
    }

    @Test
    void clientCannotDeleteAnotherUsersComment() throws Exception {
        Comment comment = createComment(3L);

        mockMvc.perform(delete("/comments/{id}", comment.getCommentId()).cookie(clientCookie))
                .andExpect(status().isForbidden());

        org.junit.jupiter.api.Assertions.assertTrue(commentRepository.findById(comment.getCommentId()).isPresent());
    }

    @Test
    void unauthenticatedCommentDeletionIsUnauthorized() throws Exception {
        Comment comment = createComment(2L);

        mockMvc.perform(delete("/comments/{id}", comment.getCommentId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletingMissingCommentReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/comments/{id}", Long.MAX_VALUE).cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingClientRemovesUnreferencedClientUser() throws Exception {
        String username = "delete-client-user-" + java.util.UUID.randomUUID();
        mockMvc.perform(post("/clients").cookie(adminCookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessName\":\"Disposable Client\",\"fullName\":\"Disposable Client\","
                                + "\"email\":\"" + username + "@example.com\",\"username\":\"" + username
                                + "\",\"password\":\"password\",\"phone\":\"0501234567\"}"))
                .andExpect(status().isCreated());

        com.otzar.sscm.entities.User user = userRepository.findByUsername(username).orElseThrow();
        Client client = clientRepository.findByUserId(user.getUser_id()).orElseThrow();

        mockMvc.perform(delete("/clients/{id}", client.getClient_id()).cookie(adminCookie))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.findById(user.getUser_id()).isEmpty());
    }

    @Test
    void deletingMissingClientReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/clients/{id}", Long.MAX_VALUE).cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientWithContentCannotBePermanentlyDeletedAndContentIsUntouched() throws Exception {
        Client client = createClient(2L);
        Content content = new Content();
        content.setClientId(client.getClient_id());
        content.setTitle("Preserved client content");
        content.setContent_type("TEXT");
        content.setStatus(ContentStatus.DRAFT);
        contentRepository.save(content);

        mockMvc.perform(delete("/clients/{id}", client.getClient_id()).cookie(adminCookie))
                .andExpect(status().isConflict());

        org.junit.jupiter.api.Assertions.assertTrue(clientRepository.findById(client.getClient_id()).isPresent());
        org.junit.jupiter.api.Assertions.assertTrue(contentRepository.findById(content.getContent_id()).isPresent());
    }

    @Test
    void adminCanArchiveAndRestoreClientWithoutChangingContentRelationship() throws Exception {
        Client client = createClient(2L);
        client.setInstagramUsername("archive.kept");
        clientRepository.save(client);
        Content content = new Content();
        content.setClientId(client.getClient_id());
        content.setTitle("Archived history");
        content.setContent_type("TEXT");
        content.setStatus(ContentStatus.DRAFT);
        contentRepository.save(content);

        mockMvc.perform(put("/clients/{id}/archive", client.getClient_id()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
        org.junit.jupiter.api.Assertions.assertTrue(clientRepository.findActiveById(client.getClient_id()).isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(client.getClient_id(),
                contentRepository.findById(content.getContent_id()).orElseThrow().getClientId());
        org.junit.jupiter.api.Assertions.assertEquals("archive.kept",
                clientRepository.findById(client.getClient_id()).orElseThrow().getInstagramUsername());

        mockMvc.perform(put("/clients/{id}/restore", client.getClient_id()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));
        org.junit.jupiter.api.Assertions.assertTrue(clientRepository.findActiveById(client.getClient_id()).isPresent());
        org.junit.jupiter.api.Assertions.assertEquals("archive.kept",
                clientRepository.findById(client.getClient_id()).orElseThrow().getInstagramUsername());
    }

    @Test
    void archivedClientIsExcludedFromActiveListAndCannotReceiveNewContent() throws Exception {
        Client client = createClient(2L);
        mockMvc.perform(put("/clients/{id}/archive", client.getClient_id()).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/clients").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.client_id == " + client.getClient_id() + ")]").isEmpty());
        mockMvc.perform(get("/clients/archived").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.client_id == " + client.getClient_id() + ")]").exists());
        mockMvc.perform(post("/contents").cookie(adminCookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + client.getClient_id()
                                + ",\"title\":\"Forbidden archived content\",\"content_type\":\"TEXT\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/clients/{id}/restore", client.getClient_id()).cookie(adminCookie))
                .andExpect(status().isOk());
    }

    @Test
    void clientCannotArchiveOrRestoreClients() throws Exception {
        Client client = createClient(3L);
        mockMvc.perform(put("/clients/{id}/archive", client.getClient_id()).cookie(clientCookie))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/clients/{id}/restore", client.getClient_id()).cookie(clientCookie))
                .andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertFalse(clientRepository.findById(client.getClient_id()).orElseThrow().isArchived());
    }

    @Test
    void deletingClientNeverDeletesAdminUser() throws Exception {
        Client client = new Client();
        client.setUser_id(1L);
        client.setBusiness_name("Admin-linked client record");
        clientRepository.save(client);

        mockMvc.perform(delete("/clients/{id}", client.getClient_id()).cookie(adminCookie))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertTrue(userRepository.findById(1L).isPresent());
    }

    @Test
    void deletedCommentCanNoLongerBeRetrieved() throws Exception {
        Comment comment = createComment(2L);

        mockMvc.perform(delete("/comments/{id}", comment.getCommentId()).cookie(clientCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/comments/by-content")
                        .param("contentId", comment.getContentId().toString())
                        .cookie(clientCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void notificationOwnerCanMarkReadButOtherUserCannot() throws Exception {
        Content content = createContent(ContentStatus.DRAFT);
        mockMvc.perform(put("/contents/{id}/send-for-approval", content.getContent_id()).cookie(adminCookie))
                .andExpect(status().isOk());
        Long notificationId = notificationRepository.findByUserId(2L).get(0).getNotificationId();

        mockMvc.perform(put("/notifications/{id}/read", notificationId).cookie(adminCookie))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/notifications/{id}/read", notificationId).cookie(clientCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void unreadCountAndMarkAllReadAreScopedToCurrentUser() throws Exception {
        Content clientNotification = createContent(ContentStatus.DRAFT);
        mockMvc.perform(put("/contents/{id}/send-for-approval", clientNotification.getContent_id()).cookie(adminCookie))
                .andExpect(status().isOk());
        Content adminNotification = createContent(ContentStatus.WAITING_APPROVAL);
        mockMvc.perform(put("/contents/{id}/approve", adminNotification.getContent_id()).cookie(clientCookie))
                .andExpect(status().isOk());

        mockMvc.perform(put("/notifications/read-all").cookie(clientCookie)).andExpect(status().isNoContent());
        mockMvc.perform(get("/notifications/unread-count").cookie(clientCookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        mockMvc.perform(get("/notifications/unread-count").cookie(adminCookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count", org.hamcrest.Matchers.greaterThan(0)));
    }

    private Content createContent(ContentStatus status) {
        return createContentForUser(2L, status);
    }

    private Comment createComment(Long userId) {
        Content content = createContentForUser(userId, ContentStatus.DRAFT);
        Comment comment = new Comment();
        comment.setContentId(content.getContent_id());
        comment.setUserId(userId);
        comment.setCommentText("Comment deletion test");
        return commentRepository.save(comment);
    }

    private Content createContentForUser(Long userId, ContentStatus status) {
        Client client = clientRepository.findByUserId(userId).orElseGet(() -> createClient(userId));

        Content content = new Content();
        content.setClientId(client.getClient_id());
        content.setTitle("Approval flow content");
        content.setDescription("Content used by approval flow tests");
        content.setFile_url("https://example.com/content.jpg");
        content.setContent_type("IMAGE");
        content.setStatus(status);

        return contentRepository.save(content);
    }

    private Client createClient() {
        return createClient(2L);
    }

    private Client createClient(Long userId) {
        Client client = new Client();
        client.setUser_id(userId);
        client.setAdmin_id(null);
        client.setBusiness_name("Approval Flow Client");
        client.setPhone("0500000001");

        return clientRepository.save(client);
    }

    private String loginToken(String username, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody).get("token").asText();
    }

    private Cookie tokenCookie(String token) {
        return new Cookie("token", token);
    }

}
