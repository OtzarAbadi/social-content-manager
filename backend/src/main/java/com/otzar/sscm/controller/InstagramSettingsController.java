package com.otzar.sscm.controller;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.InstagramSettingsRequest;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.InstagramConnectionSettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/instagram/settings")
public class InstagramSettingsController {
    private final InstagramConnectionSettingsService settingsService;
    private final AuthService authService;

    public InstagramSettingsController(InstagramConnectionSettingsService settingsService, AuthService authService) {
        this.settingsService = settingsService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<?> get(@CookieValue(value = "token", required = false) String token) {
        Optional<User> user = authService.findUserByToken(token);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authService.isAdmin(user.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(settingsService.get());
    }

    @PutMapping
    public ResponseEntity<?> update(@CookieValue(value = "token", required = false) String token,
                                    @Valid @RequestBody InstagramSettingsRequest request) {
        Optional<User> user = authService.findUserByToken(token);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authService.isAdmin(user.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(settingsService.update(request));
    }
}
