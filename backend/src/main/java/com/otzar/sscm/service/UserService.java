package com.otzar.sscm.service;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.BasicResponse;
import com.otzar.sscm.models.LoginRequest;
import com.otzar.sscm.models.LoginResponse;
import com.otzar.sscm.models.SocialManagerResponse;
import com.otzar.sscm.repository.AdminRepository;
import com.otzar.sscm.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AdminRepository adminRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminRepository = adminRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<SocialManagerResponse> findSocialManagers() {
        return adminRepository.findAll().stream()
                .map(admin -> userRepository.findById(admin.getUserId())
                        .map(user -> new SocialManagerResponse(admin, user))
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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

        if (user == null || !passwordEncoder.matches(password, user.getPassword())
                || ("CLIENT".equalsIgnoreCase(user.getRole())
                && userRepository.isArchivedClientUser(user.getUser_id()))) {
            return null;
        }

        user.setToken(UUID.randomUUID().toString());
        return userRepository.save(user);
    }

    public User findByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        User user = userRepository.findByToken(token).orElse(null);
        if (user != null && "CLIENT".equalsIgnoreCase(user.getRole())
                && userRepository.isArchivedClientUser(user.getUser_id())) {
            return null;
        }
        return user;
    }
}
