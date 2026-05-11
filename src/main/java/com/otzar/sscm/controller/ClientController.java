package com.otzar.sscm.controller;

import com.otzar.sscm.model.ClientEntity;
import com.otzar.sscm.repository.ClientRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientRepository clientRepository;

    public ClientController(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @GetMapping
    public List<ClientEntity> getAllClients() {
        return clientRepository.findAll();
    }
}