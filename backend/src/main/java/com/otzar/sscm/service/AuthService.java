package com.otzar.sscm.service;

import com.otzar.sscm.entities.Client;
import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String CLIENT_ROLE = "CLIENT";

    private final UserService userService;
    private final ClientService clientService;

    public AuthService(UserService userService, ClientService clientService) {
        this.userService = userService;
        this.clientService = clientService;
    }

    public Optional<User> findUserByToken(String token) {
        return Optional.ofNullable(userService.findByToken(token));
    }

    public boolean isAdmin(User user) {
        return user != null && !CLIENT_ROLE.equalsIgnoreCase(user.getRole());
    }

    public boolean isClient(User user) {
        return user != null && CLIENT_ROLE.equalsIgnoreCase(user.getRole());
    }

    public Optional<Client> findClientForUser(User user) {
        if (!isClient(user)) {
            return Optional.empty();
        }

        return clientService.findByUserId(user.getUser_id());
    }

    public Optional<Long> findClientIdForUser(User user) {
        return findClientForUser(user).map(Client::getClient_id);
    }

    public boolean canAccessClient(User user, Long clientId) {
        if (isAdmin(user)) {
            return true;
        }

        return findClientIdForUser(user)
                .map(id -> id.equals(clientId))
                .orElse(false);
    }

    public boolean canAccessContent(User user, Content content) {
        if (content == null) {
            return false;
        }

        return canAccessClient(user, content.getClientId());
    }
}
