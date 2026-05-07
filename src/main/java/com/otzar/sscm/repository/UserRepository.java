package com.otzar.sscm.repository;

import com.otzar.sscm.model.User;
import com.otzar.sscm.service.Persist;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepository {

    private final Persist persist;

    public UserRepository(Persist persist) {
        this.persist = persist;
    }

    public List<User> findAll() {
        return persist.loadList(User.class);
    }
}
