package com.otzar.sscm.controller;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.BasicResponse;
import com.otzar.sscm.models.LoginRequest;
import com.otzar.sscm.models.UserMeResponse;
import com.otzar.sscm.models.SocialManagerResponse;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.UserService;
import com.otzar.sscm.config.AuthCookieProperties;
import com.otzar.sscm.models.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final AuthCookieProperties cookieProperties;

    public UserController(UserService userService, AuthService authService,
                          AuthCookieProperties cookieProperties) {
        this.userService = userService;
        this.authService = authService;
        this.cookieProperties = cookieProperties;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(
            @CookieValue(value = "token", required = false) String token) {
        User currentUser = userService.findByToken(token);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!authService.isAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/social-managers")
    public ResponseEntity<List<SocialManagerResponse>> getSocialManagers(
            @CookieValue(value = "token", required = false) String token) {
        User currentUser = userService.findByToken(token);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authService.isAdmin(currentUser)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(userService.findSocialManagers());
    }

    @PostMapping("/login")
    public ResponseEntity<BasicResponse> login(@Valid @RequestBody LoginRequest request) {
        BasicResponse response = userService.login(request);
        if (!(response instanceof LoginResponse)) {
            return ResponseEntity.ok(response);
        }

        LoginResponse login = (LoginResponse) response;
        String token = login.getToken();
        if (!cookieProperties.isExposeToken()) login.setToken(null);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieProperties.authenticated(token).toString())
                .body(login);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieProperties.expired().toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@CookieValue(value = "token", required = false) String token) {
        User user = userService.findByToken(token);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new BasicResponse(false, 401));
        }

        Long clientId = authService.findClientIdForUser(user).orElse(null);

        return ResponseEntity.ok(new UserMeResponse(user, clientId));
    }
}

