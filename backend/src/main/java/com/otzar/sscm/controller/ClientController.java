package com.otzar.sscm.controller;

import com.otzar.sscm.entities.Client;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.models.CreateClientRequest;
import com.otzar.sscm.models.UpdateClientRequest;
import com.otzar.sscm.service.AuthService;
import com.otzar.sscm.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;
    private final AuthService authService;

    public ClientController(ClientService clientService, AuthService authService) {
        this.clientService = clientService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<Client>> getAllClients(@CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = currentUser.get();

        if (authService.isAdmin(user)) {
            return ResponseEntity.ok(clientService.findAll());
        }

        return ResponseEntity.ok(authService.findClientForUser(user)
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList));
    }

    @GetMapping("/archived")
    public ResponseEntity<List<Client>> getArchivedClients(@CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);
        if (currentUser.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authService.isAdmin(currentUser.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(clientService.findArchived());
    }

    @GetMapping("/{id}/content-count")
    public ResponseEntity<?> getClientContentCount(@PathVariable Long id,
                                                    @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);
        if (currentUser.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authService.isAdmin(currentUser.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return clientService.contentCount(id)
                .<ResponseEntity<?>>map(count -> ResponseEntity.ok(Map.of("count", count)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<Client> archiveClient(@PathVariable Long id,
                                                @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);
        if (currentUser.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authService.isAdmin(currentUser.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return clientService.archive(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<Client> restoreClient(@PathVariable Long id,
                                                @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);
        if (currentUser.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authService.isAdmin(currentUser.get())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return clientService.restore(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Client> getClientById(@PathVariable Long id,
                                                @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Client> client = clientService.findById(id);

        if (client.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!authService.canAccessClient(currentUser.get(), id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(client.get());
    }

    @PostMapping
    public ResponseEntity<Client> addClient(@Valid @RequestBody CreateClientRequest request,
                                            @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(@PathVariable Long id,
                                               @Valid @RequestBody UpdateClientRequest request,
                                               @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return clientService.update(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id,
                                             @CookieValue(value = "token", required = false) String token) {
        Optional<User> currentUser = authService.findUserByToken(token);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!authService.isAdmin(currentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ClientService.DeleteResult result = clientService.delete(id);
        if (result == ClientService.DeleteResult.NOT_FOUND) {
            return ResponseEntity.notFound().build();
        }
        if (result == ClientService.DeleteResult.HAS_CONTENT) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.noContent().build();
    }
}
