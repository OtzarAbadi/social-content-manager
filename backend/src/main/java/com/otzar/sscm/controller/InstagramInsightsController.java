package com.otzar.sscm.controller;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.ApiResponse;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.InstagramInsightsService;
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

    public InstagramInsightsController(InstagramInsightsService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping("/account")
    public ResponseEntity<?> account(@CookieValue(value="token",required=false) String token,
       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since,
       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate until,
       @RequestParam(defaultValue="day") String period) {
        ResponseEntity<?> denied=authorize(token); if(denied!=null)return denied;
        return ResponseEntity.ok(service.account(since,until,period));
    }

    @GetMapping("/media")
    public ResponseEntity<?> media(@CookieValue(value="token",required=false) String token,
       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since,
       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate until,
       @RequestParam(defaultValue="ALL") String mediaType, @RequestParam(defaultValue="25") int limit,
       @RequestParam(required=false) String after) {
        ResponseEntity<?> denied=authorize(token); if(denied!=null)return denied;
        return ResponseEntity.ok(service.media(since,until,mediaType.toUpperCase(Locale.ROOT),limit,after));
    }

    @GetMapping("/media/{mediaId}")
    public ResponseEntity<?> mediaItem(@CookieValue(value="token",required=false) String token,
                                      @PathVariable String mediaId) {
        ResponseEntity<?> denied=authorize(token); if(denied!=null)return denied;
        return ResponseEntity.ok(service.oneMedia(mediaId));
    }

    private ResponseEntity<?> authorize(String token) {
        Optional<User> user=authService.findUserByToken(token);
        if(user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse(false,"Authentication required"));
        boolean otzarCustomer = authService.findClientForUser(user.get())
                .map(client -> "Otzar".equalsIgnoreCase(client.getBusiness_name()))
                .orElse(false);
        if(!authService.isAdmin(user.get()) && !otzarCustomer)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false,"Instagram analytics belongs to the Otzar customer"));
        return null;
    }
}
