package com.otzar.sscm.controller;

import com.otzar.sscm.model.SocialManagerEntity;
import com.otzar.sscm.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<SocialManagerEntity> getAllUsers() {
        return userRepository.findAll();
    }
}