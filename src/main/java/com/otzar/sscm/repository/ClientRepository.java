package com.otzar.sscm.repository;

import com.otzar.sscm.model.ClientEntity;
import com.otzar.sscm.service.Persist;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ClientRepository {

    private final Persist persist;

    public ClientRepository(Persist persist) {
        this.persist = persist;
    }

    public List<ClientEntity> findAll() {
        return persist.loadList(ClientEntity.class);
    }
}
