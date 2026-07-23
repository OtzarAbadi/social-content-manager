package com.otzar.sscm.controller;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.PublishingStatusResponse;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.PublishingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
public class PublishingStatusController {
    private final AuthService auth;
    private final PublishingProvider provider;
    private final boolean schedulingEnabled;
    private final long fixedDelay;

    public PublishingStatusController(AuthService auth, PublishingProvider provider,
            @Value("${sscm.publishing.scheduling.enabled:false}") boolean enabled,
            @Value("${sscm.publishing.scheduling.fixed-delay-ms:60000}") long fixedDelay) {
        this.auth = auth; this.provider = provider;
        this.schedulingEnabled = enabled; this.fixedDelay = fixedDelay;
    }

    @GetMapping("/publishing/status")
    public ResponseEntity<PublishingStatusResponse> status(
            @CookieValue(value = "token", required = false) String token) {
        Optional<User> user = auth.findUserByToken(token);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!auth.isAdmin(user.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(new PublishingStatusResponse(
                provider.getType(), schedulingEnabled, fixedDelay));
    }
}
