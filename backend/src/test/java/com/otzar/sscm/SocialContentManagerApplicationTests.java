package com.otzar.sscm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.otzar.sscm.entities.Client;
import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.repository.ClientRepository;
import com.otzar.sscm.repository.ContentRepository;
import com.otzar.sscm.service.FileStorageService;
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

import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        mockMvc.perform(put("/contents/{id}/reject", content.getContent_id()).cookie(clientCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
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

    private Content createContent(ContentStatus status) {
        return createContentForUser(2L, status);
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
