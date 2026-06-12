package com.otzar.sscm.controller;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.ContentService;
import com.otzar.sscm.service.ContentService.ContentOperationResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/contents")
public class ContentController {

    private final ContentService contentService;
    private final AuthService authService;

    public ContentController(ContentService contentService, AuthService authService) {
        this.contentService = contentService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<Content>> getAllContents(@CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = currentUser.get();

        if (authService.isAdmin(user)) {
            return ResponseEntity.ok(contentService.findAll());
        }

        return authService.findClientIdForUser(user)
                .map(clientId -> ResponseEntity.ok(contentService.findByClientId(clientId)))
                .orElseGet(() -> ResponseEntity.ok(Collections.emptyList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Content> getContentById(@PathVariable Long id,
                                                  @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Content> content = contentService.findById(id);

        if (content.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!authService.canAccessContent(currentUser.get(), content.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(content.get());
    }

    @GetMapping("/by-client")
    public ResponseEntity<List<Content>> getContentsByClient(@RequestParam Long clientId,
                                                             @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authService.canAccessClient(currentUser.get(), clientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(contentService.findByClientId(clientId));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Content>> getContentsByClientId(@PathVariable Long clientId,
                                                               @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authService.canAccessClient(currentUser.get(), clientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return contentService.findByClientIdIfClientExists(clientId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Content>> getContentsByStatus(@PathVariable String status,
                                                             @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ContentStatus contentStatus = parseStatus(status);
            User user = currentUser.get();

            if (authService.isAdmin(user)) {
                return ResponseEntity.ok(contentService.findByStatus(contentStatus));
            }

            return authService.findClientIdForUser(user)
                    .map(clientId -> ResponseEntity.ok(contentService.findByClientIdAndStatus(clientId, contentStatus)))
                    .orElseGet(() -> ResponseEntity.ok(Collections.emptyList()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Content> addContent(@RequestBody Content content,
                                              @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ContentOperationResult result;

        try {
            result = contentService.create(content);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }

        if (!result.isSuccess()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result.getContent());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Content> updateContent(@PathVariable Long id,
                                                 @RequestBody Content content,
                                                 @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ContentOperationResult result;

        try {
            result = contentService.update(id, content);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }

        if (!result.isSuccess()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result.getContent());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable Long id,
                                              @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!contentService.delete(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Content> updateStatus(@PathVariable Long id,
                                                @RequestParam String status,
                                                @CookieValue(value = "token", required = false) String token) {
        ContentStatus requestedStatus;

        try {
            requestedStatus = parseStatus(status);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }

        if (requestedStatus == ContentStatus.APPROVED) {
            return clientStatusChange(id, token, () -> contentService.approve(id));
        }

        if (requestedStatus == ContentStatus.REJECTED) {
            return clientStatusChange(id, token, () -> contentService.reject(id));
        }

        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            return contentService.updateStatus(id, requestedStatus.name())
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/send-for-approval")
    public ResponseEntity<Content> sendForApproval(@PathVariable Long id,
                                                   @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return changeStatus(() -> contentService.sendForApproval(id));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Content> approve(@PathVariable Long id,
                                           @CookieValue(value = "token", required = false) String token) {
        return clientStatusChange(id, token, () -> contentService.approve(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Content> reject(@PathVariable Long id,
                                          @CookieValue(value = "token", required = false) String token) {
        return clientStatusChange(id, token, () -> contentService.reject(id));
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<Content> publish(@PathVariable Long id,
                                           @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return changeStatus(() -> contentService.publish(id));
    }

    private ResponseEntity<Content> clientStatusChange(Long id, String token, ContentStatusOperation operation) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authService.isClient(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Content> content = contentService.findById(id);

        if (content.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!authService.canAccessContent(currentUser.get(), content.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return changeStatus(operation);
    }

    private ContentStatus parseStatus(String status) {
        return ContentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
    }

    private ResponseEntity<Content> changeStatus(ContentStatusOperation operation) {
        try {
            return operation.execute()
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    private interface ContentStatusOperation {
        Optional<Content> execute();
    }
}
