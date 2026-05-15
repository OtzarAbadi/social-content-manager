package com.otzar.sscm.repository;

import com.otzar.sscm.entities.Client;
import com.otzar.sscm.service.Persist;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ClientRepository {

    private final Persist persist;

    public ClientRepository(Persist persist) {
        this.persist = persist;
    }

    public List<Client> findAll() {
        return persist.loadList(Client.class);
    }
}
