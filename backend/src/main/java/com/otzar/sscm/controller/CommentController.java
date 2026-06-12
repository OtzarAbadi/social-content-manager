package com.otzar.sscm.controller;

import com.otzar.sscm.entities.Comment;
import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.CommentService;
import com.otzar.sscm.service.ContentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final ContentService contentService;
    private final AuthService authService;

    public CommentController(CommentService commentService, ContentService contentService, AuthService authService) {
        this.commentService = commentService;
        this.contentService = contentService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<Comment> addComment(@RequestBody Comment comment,
                                              @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Content> content = contentService.findById(comment.getContentId());

        if (content.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!authService.canAccessContent(currentUser.get(), content.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        comment.setUserId(currentUser.get().getUser_id());
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(comment));
    }

    @GetMapping
    public ResponseEntity<List<Comment>> getAllComments(@CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = currentUser.get();

        if (authService.isAdmin(user)) {
            return ResponseEntity.ok(commentService.findAll());
        }

        return ResponseEntity.ok(commentService.findAll().stream()
                .filter(comment -> contentService.findById(comment.getContentId())
                        .map(content -> authService.canAccessContent(user, content))
                        .orElse(false))
                .collect(Collectors.toList()));
    }

    @GetMapping("/by-content")
    public ResponseEntity<List<Comment>> getCommentsByContent(@RequestParam Long contentId,
                                                              @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Content> content = contentService.findById(contentId);

        if (content.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!authService.canAccessContent(currentUser.get(), content.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(commentService.findByContentId(contentId));
    }
}
