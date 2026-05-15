package com.otzar.sscm.controller;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.BasicResponse;
import com.otzar.sscm.models.LoginResponse;
import com.otzar.sscm.repository.UserRepository;
import com.otzar.sscm.service.Persist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    private Persist persist;

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


    @RequestMapping("/login")
    public BasicResponse login (String username, String password) {
        if (password.equals("1234")) {

            User user = this.persist.login(username);

            if (user != null) {

                user.setToken("12345");

                return new LoginResponse(user);
            }

            return new BasicResponse(false, 100);
        }

        return new BasicResponse(false, 101);
    }
}

