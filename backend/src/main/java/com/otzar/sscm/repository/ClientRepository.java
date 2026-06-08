package com.otzar.sscm.repository;

import com.otzar.sscm.entities.Client;
import com.otzar.sscm.service.Persist;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class ClientRepository {

    private final Persist persist;

    public ClientRepository(Persist persist) {
        this.persist = persist;
    }

    public List<Client> findAll() {
        return persist.loadList(Client.class);
    }

    public Optional<Client> findById(Long clientId) {
        return Optional.ofNullable(persist.getQuerySession()
                .createQuery("FROM Client WHERE client_id = :clientId", Client.class)
                .setParameter("clientId", clientId)
                .uniqueResult());
    }

    public Client save(Client client) {
        persist.save(client);
        return client;
    }

    public void delete(Client client) {
        persist.remove(client);
    }
}
