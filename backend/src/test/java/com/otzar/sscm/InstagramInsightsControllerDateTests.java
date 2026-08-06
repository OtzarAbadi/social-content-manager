package com.otzar.sscm;

import com.otzar.sscm.controller.ApiExceptionHandler;
import com.otzar.sscm.controller.InstagramInsightsController;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.entities.Client;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.InstagramInsightsService;
import com.otzar.sscm.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InstagramInsightsControllerDateTests {
    private InstagramInsightsService insightsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        insightsService = mock(InstagramInsightsService.class);
        AuthService authService = mock(AuthService.class);
        User admin = new User();
        admin.setRole("ADMIN");
        when(authService.findUserByToken("admin-token")).thenReturn(Optional.of(admin));
        when(authService.isAdmin(admin)).thenReturn(true);
        when(authService.canAccessClient(admin, 1L)).thenReturn(true);
        Client client = new Client(); client.setClient_id(1L);
        ClientService clientService = mock(ClientService.class);
        when(clientService.findById(1L)).thenReturn(Optional.of(client));
        when(insightsService.account(anyLong(), any(), any(), anyString())).thenReturn(Collections.emptyMap());
        when(insightsService.media(anyLong(), any(), any(), anyString(), anyInt(), nullable(String.class)))
                .thenReturn(Collections.emptyMap());

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new InstagramInsightsController(insightsService, authService, clientService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void accountParsesIsoSinceAndUntil() throws Exception {
        mockMvc.perform(get("/instagram/insights/account")
                        .cookie(new javax.servlet.http.Cookie("token", "admin-token"))
                        .param("clientId", "1")
                        .param("since", "2026-07-21")
                        .param("until", "2026-07-27"))
                .andExpect(status().isOk());

        verify(insightsService).account(1L,
                LocalDate.of(2026, 7, 21), LocalDate.of(2026, 7, 27), "day");
    }

    @Test
    void mediaParsesIsoSinceAndUntil() throws Exception {
        mockMvc.perform(get("/instagram/insights/media")
                        .cookie(new javax.servlet.http.Cookie("token", "admin-token"))
                        .param("clientId", "1")
                        .param("since", "2026-07-21")
                        .param("until", "2026-07-27"))
                .andExpect(status().isOk());

        verify(insightsService).media(1L,
                LocalDate.of(2026, 7, 21), LocalDate.of(2026, 7, 27), "ALL", 25, null);
    }

    @Test
    void invalidDateReturnsSafeBadRequest() throws Exception {
        mockMvc.perform(get("/instagram/insights/account")
                        .cookie(new javax.servlet.http.Cookie("token", "admin-token"))
                        .param("clientId", "1")
                        .param("since", "2026-99-40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Invalid value for query parameter: since"));

        verifyNoInteractions(insightsService);
    }

    @Test
    void optionalDatesMayBeMissing() throws Exception {
        mockMvc.perform(get("/instagram/insights/account")
                        .cookie(new javax.servlet.http.Cookie("token", "admin-token"))
                        .param("clientId", "1"))
                .andExpect(status().isOk());

        verify(insightsService).account(1L, null, null, "day");
    }
}
