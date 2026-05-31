package com.otzar.sscm.service;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.BasicResponse;
import com.otzar.sscm.models.LoginRequest;
import com.otzar.sscm.models.LoginResponse;
import com.otzar.sscm.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public BasicResponse login(LoginRequest request) {
        User user = login(request.getUsername(), request.getPassword());

        if (user == null) {
            return new BasicResponse(false, 100);
        }

        return new LoginResponse(user);
    }

    public User login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null || !password.equals(user.getPassword())) {
            return null;
        }

        user.setToken(UUID.randomUUID().toString());
        return userRepository.save(user);
    }

    public User findByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        return userRepository.findByToken(token).orElse(null);
    }
}
