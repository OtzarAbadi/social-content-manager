package com.otzar.sscm;

import com.otzar.sscm.controller.CaptionSuggestionController;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.CaptionLanguage;
import com.otzar.sscm.models.CaptionSuggestionRequest;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.CaptionSuggestionService;
import com.otzar.sscm.service.ClientService;
import com.otzar.sscm.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaptionSuggestionControllerTests {
    @Test
    void authenticatedUserWithNullRoleIsForbidden() {
        UserService userService = mock(UserService.class);
        AuthService authService = new AuthService(userService, mock(ClientService.class));
        CaptionSuggestionService suggestionService = mock(CaptionSuggestionService.class);
        User user = new User();
        user.setRole(null);
        when(userService.findByToken("null-role-token")).thenReturn(user);

        CaptionSuggestionRequest request = new CaptionSuggestionRequest();
        request.setTitle("כותרת");
        request.setLanguage(CaptionLanguage.HE);

        assertEquals(HttpStatus.FORBIDDEN,
                new CaptionSuggestionController(suggestionService, authService)
                        .suggest(request, "null-role-token").getStatusCode());
    }
}
