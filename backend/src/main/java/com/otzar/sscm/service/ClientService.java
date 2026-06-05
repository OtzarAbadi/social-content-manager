package com.otzar.sscm.service;

import com.otzar.sscm.entities.Client;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.CreateClientRequest;
import com.otzar.sscm.repository.ClientRepository;
import com.otzar.sscm.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public ClientService(ClientRepository clientRepository, UserRepository userRepository) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    public Client create(CreateClientRequest request) {
        User user = new User();
        user.setFull_name(valueOrFallback(request.getFullName(), request.getBusinessName()));
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRole("CLIENT");
        user.setToken("");

        userRepository.save(user);

        Client client = new Client();
        client.setUser_id(user.getUser_id());
        client.setAdmin_id(request.getAdminId());
        client.setBusiness_name(request.getBusinessName());
        client.setPhone(request.getPhone());

        return clientRepository.save(client);
    }

    private String valueOrFallback(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value;
    }
}
