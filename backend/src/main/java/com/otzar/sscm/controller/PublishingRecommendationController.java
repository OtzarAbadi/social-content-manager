package com.otzar.sscm.controller;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.PublishingRecommendationRequest;
import com.otzar.sscm.models.PublishingRecommendationResponse;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.PublishingRecommendationService;
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
@RequestMapping("/contents/publishing-recommendations")
public class PublishingRecommendationController {
    private final PublishingRecommendationService recommendationService;
    private final AuthService authService;

    public PublishingRecommendationController(PublishingRecommendationService recommendationService,
                                              AuthService authService) {
        this.recommendationService = recommendationService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<PublishingRecommendationResponse> recommend(
            @Valid @RequestBody PublishingRecommendationRequest request,
            @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!"ADMIN".equals(currentUser.get().getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(recommendationService.recommend(request));
    }
}
