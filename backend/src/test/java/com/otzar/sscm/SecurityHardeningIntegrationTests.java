package com.otzar.sscm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.repository.UserRepository;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.PasswordMigrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.Cookie;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PasswordMigrationService passwordMigrationService;
    @Autowired private AuthService authService;

    private Cookie adminCookie;
    private Cookie clientCookie;

    @BeforeEach
    void setUp() throws Exception {
        adminCookie = loginCookie("admin", "123456");
        clientCookie = loginCookie("client1", "123456");
    }

    @Test
    void newlyCreatedUserPasswordIsBcryptHashed() throws Exception {
        String username = unique("created");
        createClient(username, "new-password");

        String stored = userRepository.findByUsername(username).orElseThrow().getPassword();
        assertTrue(passwordMigrationService.isBcryptHash(stored));
        assertTrue(passwordEncoder.matches("new-password", stored));
    }

    @Test
    void storedPasswordDoesNotEqualRawPassword() throws Exception {
        String username = unique("raw-check");
        createClient(username, "raw-password");

        assertNotEquals("raw-password",
                userRepository.findByUsername(username).orElseThrow().getPassword());
    }

    @Test
    void loginSucceedsWithCorrectPassword() throws Exception {
        mockMvc.perform(loginRequest("admin", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        mockMvc.perform(loginRequest("admin", "wrong-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(100));
    }

    @Test
    void existingPlaintextPasswordIsMigratedAndStillWorks() throws Exception {
        String username = unique("legacy");
        User user = saveUser(username, "legacy-password", "CLIENT");

        passwordMigrationService.migratePlaintextPasswords();

        String stored = userRepository.findById(user.getUser_id()).orElseThrow().getPassword();
        assertTrue(passwordMigrationService.isBcryptHash(stored));
        mockMvc.perform(loginRequest(username, "legacy-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void alreadyHashedPasswordIsNotHashedAgain() {
        String hash = passwordEncoder.encode("existing-password");
        User user = saveUser(unique("hashed"), hash, "CLIENT");

        passwordMigrationService.migratePlaintextPasswords();

        assertEquals(hash, userRepository.findById(user.getUser_id()).orElseThrow().getPassword());
    }

    @Test
    void passwordNeverAppearsInUserJson() throws Exception {
        mockMvc.perform(get("/users").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("\"password\""))));
    }

    @Test
    void storedTokenNeverAppearsInUserJson() throws Exception {
        mockMvc.perform(get("/users").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("\"token\""))));
    }

    @Test
    void unauthenticatedGetUsersIsBlocked() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clientGetUsersIsBlocked() throws Exception {
        mockMvc.perform(get("/users").cookie(clientCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminGetUsersSucceeds() throws Exception {
        mockMvc.perform(get("/users").cookie(adminCookie))
                .andExpect(status().isOk());
    }

    @Test
    void nullAndUnknownRolesAreNotTreatedAsAdmin() {
        User nullRole = new User();
        User unknownRole = new User();
        unknownRole.setRole("SUPERUSER");

        assertFalse(authService.isAdmin(nullRole));
        assertFalse(authService.isAdmin(unknownRole));
    }

    private void createClient(String username, String password) throws Exception {
        String body = "{\"businessName\":\"Secure Client\",\"fullName\":\"Secure Client\","
                + "\"email\":\"" + username + "@example.com\",\"username\":\"" + username
                + "\",\"password\":\"" + password + "\",\"phone\":\"0501234567\"}";
        mockMvc.perform(post("/clients").cookie(adminCookie).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private User saveUser(String username, String password, String role) {
        User user = new User();
        user.setFull_name("Migration User");
        user.setEmail(username + "@example.com");
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setToken("");
        return userRepository.save(user);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(
            String username, String password) {
        return post("/users/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
    }

    private Cookie loginCookie(String username, String password) throws Exception {
        String response = mockMvc.perform(loginRequest(username, password))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();
        return new Cookie("token", objectMapper.readTree(response).get("token").asText());
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
