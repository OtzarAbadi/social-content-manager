package com.otzar.sscm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.otzar.sscm.controller.UserController;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.LoginRequest;
import com.otzar.sscm.models.LoginResponse;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductionAuthCookieTests {
    @Test
    void productionCookieIsSecureHttpOnlySameSiteNoneAndTokenIsNotSerialized() throws Exception {
        User user = new User();
        user.setToken("server-only-token");
        LoginResponse login = new LoginResponse(user);
        UserService users = mock(UserService.class);
        when(users.login(org.mockito.ArgumentMatchers.any(LoginRequest.class))).thenReturn(login);

        AuthCookieProperties cookies = new AuthCookieProperties(true, "None", false);
        UserController controller = new UserController(users, mock(AuthService.class), cookies);
        ResponseEntity<?> response = controller.login(new LoginRequest());

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("SameSite=None"));

        String body = new ObjectMapper().writeValueAsString(response.getBody());
        assertFalse(body.contains("server-only-token"));
        assertFalse(body.contains("\"token\""));
    }

    @Test
    void localCookieRemainsCompatibleWithHttpDevelopment() {
        AuthCookieProperties cookies = new AuthCookieProperties(false, "Lax", true);
        String header = cookies.authenticated("local-token").toString();
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("SameSite=Lax"));
        assertFalse(header.contains("Secure"));
    }
}
