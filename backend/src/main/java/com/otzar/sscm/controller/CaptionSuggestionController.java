package com.otzar.sscm.controller;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.CaptionSuggestionRequest;
import com.otzar.sscm.models.CaptionSuggestionResponse;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.CaptionSuggestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/contents/caption-suggestions")
public class CaptionSuggestionController {
    private final CaptionSuggestionService captionSuggestionService;
    private final AuthService authService;

    public CaptionSuggestionController(CaptionSuggestionService captionSuggestionService,
                                       AuthService authService) {
        this.captionSuggestionService = captionSuggestionService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<CaptionSuggestionResponse> suggest(
            @Valid @RequestBody CaptionSuggestionRequest request,
            @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(captionSuggestionService.suggest(request));
    }
}
