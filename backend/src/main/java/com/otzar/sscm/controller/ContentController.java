package com.otzar.sscm.controller;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.entities.NotificationType;
import com.otzar.sscm.models.ApiResponse;
import com.otzar.sscm.models.CreateContentMultipartRequest;
import com.otzar.sscm.models.ContentVersionResponse;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.ContentService;
import com.otzar.sscm.service.ContentVersionService;
import com.otzar.sscm.service.FileStorageService;
import com.otzar.sscm.service.NotificationService;
import com.otzar.sscm.service.ContentService.ContentOperationResult;
import com.otzar.sscm.models.RejectContentRequest;
import com.otzar.sscm.models.UpdateScheduleRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/contents")
public class ContentController {

    private static final Logger logger = LoggerFactory.getLogger(ContentController.class);
    private final ContentService contentService;
    private final ContentVersionService contentVersionService;
    private final AuthService authService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public ContentController(ContentService contentService, ContentVersionService contentVersionService,
                             AuthService authService,
                             FileStorageService fileStorageService, NotificationService notificationService) {
        this.contentService = contentService;
        this.contentVersionService = contentVersionService;
        this.authService = authService;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
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

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<ContentVersionResponse>> getContentVersions(
            @PathVariable Long id,
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

        return ResponseEntity.ok(contentVersionService.findHistory(id));
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addContent(@Valid @RequestBody Content content,
                                              @CookieValue(value = "token", required = false) String token) {
        return createContent(content, token);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addContentWithFile(
            @Valid @ModelAttribute CreateContentMultipartRequest request,
            @CookieValue(value = "token", required = false) String token) {
        logger.info("Create content multipart request: clientId={}, titlePresent={}, filePresent={}",
                request.getClientId(),
                request.getTitle() != null && !request.getTitle().trim().isEmpty(),
                request.getFile() != null && !request.getFile().isEmpty());

        Optional<User> currentUser = authService.findUserByToken(token);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Authentication required"));
        }
        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "You are not allowed to create content"));
        }
        if (!contentService.clientExists(request.getClientId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Client not found for id: " + request.getClientId()));
        }

        Content content = new Content();
        content.setClientId(request.getClientId());
        content.setTitle(request.getTitle().trim());
        content.setDescription(request.getDescription());
        content.setContent_type(request.getContentType());
        content.setPlannedPublishDate(request.getPlannedPublishDate());

        try {
            content.setFile_url(fileStorageService.store(request.getFile()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Could not save uploaded file"));
        }

        return createContent(content, token);
    }

    private ResponseEntity<?> createContent(Content content, String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Authentication required"));
        }

        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(false, "You are not allowed to create content"));
        }

        ContentOperationResult result;

        try {
            result = contentService.create(content, currentUser.get().getUser_id());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, ex.getMessage()));
        }

        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Content must be connected to a client"));
        }

        Content created = result.getContent();
        notificationService.notifyClient(created, NotificationType.CONTENT_CREATED,
                "תוכן חדש נוצר", "נוצר עבורך תוכן חדש: " + created.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
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
            result = contentService.update(id, content, currentUser.get().getUser_id());
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

    @PutMapping("/{id}/schedule")
    public ResponseEntity<Content> updateSchedule(@PathVariable Long id,
                                                  @RequestBody UpdateScheduleRequest request,
                                                  @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (request == null || request.getPlannedPublishDate() == null) {
            return ResponseEntity.badRequest().build();
        }

        return contentService.updatePlannedPublishDate(
                        id, request.getPlannedPublishDate(), currentUser.get().getUser_id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Content> updateStatus(@PathVariable Long id,
                                                @RequestParam String status,
                                                @Valid @RequestBody(required = false) RejectContentRequest request,
                                                @CookieValue(value = "token", required = false) String token) {
        ContentStatus requestedStatus;

        try {
            requestedStatus = parseStatus(status);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }

        if (requestedStatus == ContentStatus.APPROVED) {
            return clientStatusChange(id, token, user -> contentService.approve(id, user.getUser_id()).map(content -> {
                notificationService.notifyAdmin(content, NotificationType.CONTENT_APPROVED,
                        "התוכן אושר", "הלקוח אישר את התוכן ‘" + content.getTitle() + "’");
                return content;
            }));
        }

        if (requestedStatus == ContentStatus.REJECTED) {
            return clientRejection(id, token, request);
        }

        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            return withStatusNotification(contentService.updateStatus(
                            id, requestedStatus.name(), currentUser.get().getUser_id()), requestedStatus)
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

        ContentStatus previousStatus = contentService.findById(id).map(Content::getStatus).orElse(null);
        return changeStatus(() -> contentService.sendForApproval(id, currentUser.get().getUser_id()).map(content -> {
            NotificationType type = previousStatus == ContentStatus.REJECTED
                    ? NotificationType.CONTENT_RESUBMITTED : NotificationType.CONTENT_WAITING_APPROVAL;
            String title = type == NotificationType.CONTENT_RESUBMITTED ? "התוכן הוגש מחדש" : "תוכן ממתין לאישור";
            notificationService.notifyClient(content, type, title, "התוכן ‘" + content.getTitle() + "’ מוכן לבדיקה");
            return content;
        }));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Content> approve(@PathVariable Long id,
                                           @CookieValue(value = "token", required = false) String token) {
        return clientStatusChange(id, token, user -> contentService.approve(id, user.getUser_id()).map(content -> {
            notificationService.notifyAdmin(content, NotificationType.CONTENT_APPROVED,
                    "התוכן אושר", "הלקוח אישר את התוכן ‘" + content.getTitle() + "’");
            return content;
        }));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Content> reject(@PathVariable Long id,
                                          @Valid @RequestBody RejectContentRequest request,
                                          @CookieValue(value = "token", required = false) String token) {
        return clientRejection(id, token, request);
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

        return changeStatus(() -> contentService.publish(id, currentUser.get().getUser_id()).map(content -> {
            notificationService.notifyClient(content, NotificationType.CONTENT_PUBLISHED,
                    "התוכן פורסם", "התוכן ‘" + content.getTitle() + "’ פורסם");
            return content;
        }));
    }

    private ResponseEntity<Content> clientStatusChange(Long id, String token, ClientContentStatusOperation operation) {
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

        return changeStatus(() -> operation.execute(currentUser.get()));
    }

    private ResponseEntity<Content> clientRejection(Long id, String token, RejectContentRequest request) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = currentUser.get();
        if (!authService.isClient(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Content> content = contentService.findById(id);
        if (content.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!authService.canAccessContent(user, content.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String reason = request == null ? null : request.getReason();
        return changeStatus(() -> contentService.reject(id, user.getUser_id(), reason).map(rejected -> {
            notificationService.notifyAdmin(rejected, NotificationType.CONTENT_REJECTED,
                    "התוכן נדחה", "התוכן ‘" + rejected.getTitle() + "’ נדחה. סיבה: " + reason.trim());
            return rejected;
        }));
    }

    private Optional<Content> withStatusNotification(Optional<Content> result, ContentStatus status) {
        return result.map(content -> {
            if (status == ContentStatus.WAITING_APPROVAL) {
                notificationService.notifyClient(content, NotificationType.CONTENT_WAITING_APPROVAL,
                        "תוכן ממתין לאישור", "התוכן ‘" + content.getTitle() + "’ מוכן לבדיקה");
            } else if (status == ContentStatus.PUBLISHED) {
                notificationService.notifyClient(content, NotificationType.CONTENT_PUBLISHED,
                        "התוכן פורסם", "התוכן ‘" + content.getTitle() + "’ פורסם");
            }
            return content;
        });
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

    private interface ClientContentStatusOperation {
        Optional<Content> execute(User user);
    }
}
