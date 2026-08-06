package com.otzar.sscm;

import com.otzar.sscm.controller.ApiExceptionHandler;
import com.otzar.sscm.controller.InstagramInsightsController;
import com.otzar.sscm.entities.Client;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.ClientService;
import com.otzar.sscm.service.InstagramInsightsException;
import com.otzar.sscm.service.InstagramInsightsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.Cookie;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InstagramInsightsAuthorizationTests {
    private InstagramInsightsService insights;
    private AuthService auth;
    private ClientService clients;
    private MockMvc mvc;

    @BeforeEach void setUp() {
        insights = mock(InstagramInsightsService.class);
        auth = mock(AuthService.class);
        clients = mock(ClientService.class);
        when(insights.account(anyLong(), any(), any(), anyString())).thenReturn(Collections.emptyMap());
        mvc = MockMvcBuilders.standaloneSetup(new InstagramInsightsController(insights, auth, clients))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test void adminCanSelectExistingAuthorizedClient() throws Exception {
        User admin = user("ADMIN", 1L); Client otzar = client(10L);
        when(auth.findUserByToken("a")).thenReturn(Optional.of(admin)); when(auth.isAdmin(admin)).thenReturn(true);
        when(auth.canAccessClient(admin, 10L)).thenReturn(true); when(clients.findById(10L)).thenReturn(Optional.of(otzar));
        mvc.perform(account("a").param("clientId", "10")).andExpect(status().isOk());
        verify(insights).account(eq(10L), any(), any(), eq("day"));
    }

    @Test void clientUsesOwnedClientWithoutClientId() throws Exception {
        User client = user("CLIENT", 2L); when(auth.findUserByToken("c")).thenReturn(Optional.of(client));
        when(auth.findClientIdForUser(client)).thenReturn(Optional.of(10L));
        mvc.perform(account("c")).andExpect(status().isOk());
        verify(insights).account(eq(10L), any(), any(), eq("day"));
    }

    @Test void clientCannotRequestForeignClient() throws Exception {
        User client = user("CLIENT", 2L); when(auth.findUserByToken("c")).thenReturn(Optional.of(client));
        when(auth.findClientIdForUser(client)).thenReturn(Optional.of(10L));
        mvc.perform(account("c").param("clientId", "11"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN_CLIENT_ACCESS"));
        verifyNoInteractions(insights);
    }

    @Test void missingClientAssociationIsSafe() throws Exception {
        User client = user("CLIENT", 2L); when(auth.findUserByToken("c")).thenReturn(Optional.of(client));
        when(auth.findClientIdForUser(client)).thenReturn(Optional.empty());
        mvc.perform(account("c")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
    }

    @Test void disconnectedClientReturnsSafeCode() throws Exception {
        User client = user("CLIENT", 2L); when(auth.findUserByToken("c")).thenReturn(Optional.of(client));
        when(auth.findClientIdForUser(client)).thenReturn(Optional.of(10L));
        when(insights.account(eq(10L), any(), any(), anyString())).thenThrow(new InstagramInsightsException(
                "INSTAGRAM_NOT_CONNECTED", "Instagram account is not connected for this client.", HttpStatus.NOT_FOUND));
        mvc.perform(account("c")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("INSTAGRAM_NOT_CONNECTED"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("access_token"))));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder account(String token) {
        return get("/instagram/insights/account").cookie(new Cookie("token", token));
    }
    private User user(String role, Long id) { User u = new User(); u.setRole(role); u.setUser_id(id); return u; }
    private Client client(Long id) { Client c = new Client(); c.setClient_id(id); return c; }
}
