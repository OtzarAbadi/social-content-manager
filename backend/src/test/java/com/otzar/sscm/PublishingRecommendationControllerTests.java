package com.otzar.sscm;

import com.otzar.sscm.controller.PublishingRecommendationController;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.PublishingRecommendationRequest;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.PublishingRecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublishingRecommendationControllerTests {
    @Test
    void nullAndMalformedRolesAreForbidden() {
        assertForbidden(null, "null-role-token");
        assertForbidden(" ADMIN ", "malformed-role-token");
    }

    private void assertForbidden(String role, String token) {
        AuthService authService = mock(AuthService.class);
        PublishingRecommendationService service = mock(PublishingRecommendationService.class);
        User user = new User();
        user.setRole(role);
        when(authService.findUserByToken(token)).thenReturn(Optional.of(user));

        assertEquals(HttpStatus.FORBIDDEN,
                new PublishingRecommendationController(service, authService)
                        .recommend(new PublishingRecommendationRequest(), token).getStatusCode());
    }
}
