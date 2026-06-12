package com.otzar.sscm.controller;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.BasicResponse;
import com.otzar.sscm.models.LoginRequest;
import com.otzar.sscm.models.UserMeResponse;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAll();
    }

    @PostMapping("/login")
    public BasicResponse login(@RequestBody LoginRequest request) {
        return userService.login(request);
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

