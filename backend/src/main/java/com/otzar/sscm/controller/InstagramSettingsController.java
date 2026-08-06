package com.otzar.sscm.controller;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.InstagramSettingsRequest;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.InstagramConnectionSettingsService;
import com.otzar.sscm.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Map;
import com.otzar.sscm.service.InstagramInsightsException;

@RestController
@RequestMapping("/instagram/settings")
public class InstagramSettingsController {
    private final InstagramConnectionSettingsService settingsService;
    private final AuthService authService;
    private final ClientService clientService;

    public InstagramSettingsController(InstagramConnectionSettingsService settingsService, AuthService authService, ClientService clientService) {
        this.settingsService = settingsService;
        this.authService = authService;
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<?> get(@CookieValue(value = "token", required = false) String token,
                                 @RequestParam(required = false) Long clientId) {
        Optional<User> user = authService.findUserByToken(token);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long effectiveClientId = resolveClient(user.get(), clientId);
        com.otzar.sscm.models.InstagramSettingsResponse settings = settingsService.get(effectiveClientId);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("connected", settingsService.isConnected(effectiveClientId));
        status.put("accessTokenConfigured", settings.accessTokenConfigured);
        status.put("connectionSource", "SERVER_CONFIGURATION");
        return ResponseEntity.ok(status);
    }

    @PutMapping
    public ResponseEntity<?> update(@CookieValue(value = "token", required = false) String token,
                                    @Valid @RequestBody InstagramSettingsRequest request) {
        Optional<User> user = authService.findUserByToken(token);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authService.isAdmin(user.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.badRequest().body("clientId is required");
    }

    @PutMapping(params = "clientId")
    public ResponseEntity<?> updateForClient(@CookieValue(value = "token", required = false) String token,
                                             @RequestParam Long clientId,
                                             @Valid @RequestBody InstagramSettingsRequest request) {
        Optional<User> user = authService.findUserByToken(token);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authService.isAdmin(user.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (clientService.findById(clientId).isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(settingsService.update(clientId, request));
    }

    private Long resolveClient(User user, Long requestedClientId) {
        if (authService.isAdmin(user)) {
            if (requestedClientId == null || clientService.findById(requestedClientId).isEmpty())
                throw new InstagramInsightsException("CLIENT_NOT_FOUND", "Client was not found", HttpStatus.NOT_FOUND);
            if (!authService.canAccessClient(user, requestedClientId))
                throw new InstagramInsightsException("FORBIDDEN_CLIENT_ACCESS", "Client access is forbidden", HttpStatus.FORBIDDEN);
            return requestedClientId;
        }
        Long owned = authService.findClientIdForUser(user).orElseThrow(() ->
                new InstagramInsightsException("CLIENT_NOT_FOUND", "Authenticated user has no client association", HttpStatus.NOT_FOUND));
        if (requestedClientId != null && !requestedClientId.equals(owned))
            throw new InstagramInsightsException("FORBIDDEN_CLIENT_ACCESS", "Client access is forbidden", HttpStatus.FORBIDDEN);
        return owned;
    }
}
