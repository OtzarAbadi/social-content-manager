package com.otzar.sscm.controller;

import com.otzar.sscm.entities.*;
import com.otzar.sscm.models.*;
import com.otzar.sscm.repository.PublicationRecordRepository;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.ContentService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class PublicationController {
    private final AuthService auth;
    private final ContentService contents;
    private final PublicationRecordRepository publications;

    public PublicationController(AuthService auth, ContentService contents,
                                 PublicationRecordRepository publications) {
        this.auth = auth; this.contents = contents; this.publications = publications;
    }

    @GetMapping("/publications")
    public ResponseEntity<List<PublicationResponse>> all(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) PublicationStatus status,
            @RequestParam(required = false) PublishingProviderType provider,
            @RequestParam(required = false) PublicationTriggerType triggerType,
            @CookieValue(value = "token", required = false) String token) {
        Optional<User> user = auth.findUserByToken(token);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!auth.isAdmin(user.get()) && !auth.isClient(user.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Long clientId = auth.isClient(user.get()) ? auth.findClientIdForUser(user.get()).orElse(-1L) : null;
        return ResponseEntity.ok(toResponses(publications.find(null, clientId, status, provider, triggerType, bound(limit))));
    }

    @GetMapping("/publications/{id}")
    public ResponseEntity<PublicationResponse> one(@PathVariable Long id,
            @CookieValue(value = "token", required = false) String token) {
        Optional<User> user = auth.findUserByToken(token);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!auth.isAdmin(user.get()) && !auth.isClient(user.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Optional<PublicationRecord> record = publications.findById(id);
        if (record.isEmpty()) return ResponseEntity.notFound().build();
        Optional<Content> content = contents.findById(record.get().getContentId());
        if (content.isEmpty()) return ResponseEntity.notFound().build();
        if (!auth.canAccessContent(user.get(), content.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(new PublicationResponse(record.get()));
    }

    @GetMapping("/contents/{contentId}/publications")
    public ResponseEntity<List<PublicationResponse>> forContent(@PathVariable Long contentId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) PublicationStatus status,
            @RequestParam(required = false) PublishingProviderType provider,
            @RequestParam(required = false) PublicationTriggerType triggerType,
            @CookieValue(value = "token", required = false) String token) {
        Optional<User> user = auth.findUserByToken(token);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!auth.isAdmin(user.get()) && !auth.isClient(user.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Optional<Content> content = contents.findById(contentId);
        if (content.isEmpty()) return ResponseEntity.notFound().build();
        if (!auth.canAccessContent(user.get(), content.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(toResponses(publications.find(
                contentId, null, status, provider, triggerType, bound(limit))));
    }

    private int bound(int limit) { return Math.max(1, Math.min(limit, 200)); }
    private List<PublicationResponse> toResponses(List<PublicationRecord> records) {
        return records.stream().map(PublicationResponse::new).collect(Collectors.toList());
    }
}
