package com.otzar.sscm.controller;

import com.otzar.sscm.entities.Notification;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.UnreadCountResponse;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final AuthService authService;

    public NotificationController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(@CookieValue(value = "token", required = false) String token) {
        return currentUser(token).map(user -> ResponseEntity.ok(notificationService.findForUser(user.getUser_id())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(@CookieValue(value = "token", required = false) String token) {
        return currentUser(token).map(user -> ResponseEntity.ok(
                new UnreadCountResponse(notificationService.unreadCount(user.getUser_id()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markRead(@PathVariable Long id,
                                                  @CookieValue(value = "token", required = false) String token) {
        Optional<User> user = currentUser(token);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return notificationService.markRead(id, user.get().getUser_id()).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@CookieValue(value = "token", required = false) String token) {
        Optional<User> user = currentUser(token);
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        notificationService.markAllRead(user.get().getUser_id());
        return ResponseEntity.noContent().build();
    }

    private Optional<User> currentUser(String token) { return authService.findUserByToken(token); }
}
