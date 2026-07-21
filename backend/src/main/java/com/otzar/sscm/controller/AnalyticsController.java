package com.otzar.sscm.controller;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.AnalyticsDashboardResponse;
import com.otzar.sscm.service.AnalyticsService;
import com.otzar.sscm.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final AuthService authService;

    public AnalyticsController(AnalyticsService analyticsService, AuthService authService) {
        this.analyticsService = analyticsService;
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDashboardResponse> getDashboard(
            @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = currentUser.get();
        if (authService.isAdmin(user)) {
            return ResponseEntity.ok(analyticsService.getAdminDashboard());
        }
        if (authService.isClient(user)) {
            return authService.findClientIdForUser(user)
                    .map(clientId -> ResponseEntity.ok(analyticsService.getClientDashboard(clientId)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
