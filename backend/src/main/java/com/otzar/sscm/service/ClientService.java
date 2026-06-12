package com.otzar.sscm.service;

import com.otzar.sscm.entities.Client;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.CreateClientRequest;
import com.otzar.sscm.models.UpdateClientRequest;
import com.otzar.sscm.repository.ClientRepository;
import com.otzar.sscm.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public Optional<Client> findById(Long id) {
        return clientRepository.findById(id);
    }

    public Optional<Client> findByUserId(Long userId) {
        return clientRepository.findByUserId(userId);
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

    public Optional<Client> update(Long id, UpdateClientRequest request) {
        Optional<Client> existingClient = clientRepository.findById(id);

        if (existingClient.isEmpty()) {
            return Optional.empty();
        }

        Client client = existingClient.get();

        if (request.getUserId() != null) {
            client.setUser_id(request.getUserId());
        }

        if (request.getAdminId() != null) {
            client.setAdmin_id(request.getAdminId());
        }

        if (request.getBusinessName() != null) {
            client.setBusiness_name(request.getBusinessName());
        }

        if (request.getPhone() != null) {
            client.setPhone(request.getPhone());
        }

        return Optional.of(clientRepository.save(client));
    }

    public boolean delete(Long id) {
        Optional<Client> existingClient = clientRepository.findById(id);

        if (existingClient.isEmpty()) {
            return false;
        }

        clientRepository.delete(existingClient.get());
        return true;
    }

    private String valueOrFallback(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value;
    }
}
