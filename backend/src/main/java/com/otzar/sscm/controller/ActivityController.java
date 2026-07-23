package com.otzar.sscm.controller;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.ActivityResponse;
import com.otzar.sscm.models.ActivityType;
import com.otzar.sscm.service.ActivityService;
import com.otzar.sscm.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/activity")
public class ActivityController {
    private static final int MAX_LIMIT = 100;

    private final ActivityService activityService;
    private final AuthService authService;

    public ActivityController(ActivityService activityService, AuthService authService) {
        this.activityService = activityService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getActivity(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) ActivityType type,
            @CookieValue(value = "token", required = false) String token) {
        if (limit < 1 || limit > MAX_LIMIT) {
            return ResponseEntity.badRequest().build();
        }

        Optional<User> currentUser = authService.findUserByToken(token);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = currentUser.get();
        if (authService.isAdmin(user)) {
            return ResponseEntity.ok(activityService.findActivities(null, type, limit, true));
        }
        if (authService.isClient(user)) {
            return authService.findClientIdForUser(user)
                    .map(clientId -> ResponseEntity.ok(
                            activityService.findActivities(clientId, type, limit, false)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
