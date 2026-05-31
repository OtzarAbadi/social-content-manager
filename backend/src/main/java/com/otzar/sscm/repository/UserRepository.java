package com.otzar.sscm.repository;

import com.otzar.sscm.entities.User;
import com.otzar.sscm.service.Persist;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class UserRepository {

    private final Persist persist;

    public UserRepository(Persist persist) {
        this.persist = persist;
    }

    public List<User> findAll() {
        return persist.loadList(User.class);
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable((User) persist.getQuerySession()
                .createQuery("FROM User WHERE username = :username")
                .setString("username", username)
                .uniqueResult());
    }

    public Optional<User> findByToken(String token) {
        return Optional.ofNullable((User) persist.getQuerySession()
                .createQuery("FROM User WHERE token = :token")
                .setString("token", token)
                .uniqueResult());
    }

    public User save(User user) {
        persist.save(user);
        return user;
    }
}
