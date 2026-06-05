package com.otzar.sscm.controller;

import com.otzar.sscm.entities.Client;
import com.otzar.sscm.models.CreateClientRequest;
import com.otzar.sscm.service.ClientService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public List<Client> getAllClients() {
        return clientService.findAll();
    }

    @PostMapping
    public Client addClient(@RequestBody CreateClientRequest request) {
        return clientService.create(request);
    }
}
