package com.otzar.sscm.controller;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.ApiResponse;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.InstagramInsightsService;
import com.otzar.sscm.service.InstagramInsightsException;
import com.otzar.sscm.service.ClientService;
import org.springframework.http.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/instagram/insights")
public class InstagramInsightsController {
    private final InstagramInsightsService service;
    private final AuthService authService;
    private final ClientService clientService;

    public InstagramInsightsController(InstagramInsightsService service, AuthService authService,
                                       ClientService clientService) {
        this.service = service;
        this.authService = authService;
        this.clientService = clientService;
    }

    @GetMapping("/account")
    public ResponseEntity<?> account(@CookieValue(value="token",required=false) String token,
       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since,
       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate until,
       @RequestParam(defaultValue="day") String period,
       @RequestParam(required=false) Long clientId) {
        return ResponseEntity.ok(service.account(resolveClient(token, clientId),since,until,period));
    }

    @GetMapping("/media")
    public ResponseEntity<?> media(@CookieValue(value="token",required=false) String token,
       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since,
       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate until,
       @RequestParam(defaultValue="ALL") String mediaType, @RequestParam(defaultValue="25") int limit,
       @RequestParam(required=false) String after, @RequestParam(required=false) Long clientId) {
        return ResponseEntity.ok(service.media(resolveClient(token, clientId),since,until,mediaType.toUpperCase(Locale.ROOT),limit,after));
    }

    @GetMapping("/media/{mediaId}")
    public ResponseEntity<?> mediaItem(@CookieValue(value="token",required=false) String token,
                                      @PathVariable String mediaId,
                                      @RequestParam(required=false) Long clientId) {
        return ResponseEntity.ok(service.oneMedia(resolveClient(token, clientId), mediaId));
    }

    private Long resolveClient(String token, Long requestedClientId) {
        Optional<User> user=authService.findUserByToken(token);
        if(user.isEmpty()) throw new InstagramInsightsException("UNAUTHORIZED", "Authentication required", HttpStatus.UNAUTHORIZED);
        if (authService.isAdmin(user.get())) {
            if (requestedClientId == null) throw new InstagramInsightsException("CLIENT_NOT_FOUND", "Select a client", HttpStatus.BAD_REQUEST);
            if (clientService.findById(requestedClientId).isEmpty()) throw new InstagramInsightsException("CLIENT_NOT_FOUND", "Client was not found", HttpStatus.NOT_FOUND);
            if (!authService.canAccessClient(user.get(), requestedClientId)) throw new InstagramInsightsException("FORBIDDEN_CLIENT_ACCESS", "Client access is forbidden", HttpStatus.FORBIDDEN);
            return requestedClientId;
        }
        Long owned = authService.findClientIdForUser(user.get()).orElseThrow(() ->
                new InstagramInsightsException("CLIENT_NOT_FOUND", "Authenticated user has no client association", HttpStatus.NOT_FOUND));
        if (requestedClientId != null && !requestedClientId.equals(owned))
            throw new InstagramInsightsException("FORBIDDEN_CLIENT_ACCESS", "Client access is forbidden", HttpStatus.FORBIDDEN);
        return owned;
    }
}
