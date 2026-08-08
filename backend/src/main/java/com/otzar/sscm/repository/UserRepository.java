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

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(persist.loadObject(User.class, id));
    }

    public Optional<User> findFirstAdmin() {
        return persist.getQuerySession().createQuery(
                "FROM User WHERE UPPER(role) = 'ADMIN' ORDER BY user_id", User.class)
                .setMaxResults(1).uniqueResultOptional();
    }

    public boolean isActiveClientUser(Long userId) {
        Long count = persist.getQuerySession().createQuery(
                        "SELECT COUNT(*) FROM Client WHERE user_id = :userId AND archived = false", Long.class)
                .setParameter("userId", userId)
                .uniqueResult();
        return count > 0;
    }

    public boolean isArchivedClientUser(Long userId) {
        Long count = persist.getQuerySession().createQuery(
                        "SELECT COUNT(*) FROM Client WHERE user_id = :userId AND archived = true", Long.class)
                .setParameter("userId", userId)
                .uniqueResult();
        return count > 0 && !isActiveClientUser(userId);
    }

    public User save(User user) {
        persist.save(user);
        return user;
    }

    public boolean hasReferencesOutsideClient(Long userId, Long clientId) {
        Long otherClients = persist.getQuerySession().createQuery(
                        "SELECT COUNT(*) FROM Client WHERE user_id = :userId AND client_id <> :clientId", Long.class)
                .setParameter("userId", userId)
                .setParameter("clientId", clientId)
                .uniqueResult();
        Long admins = persist.getQuerySession().createQuery(
                        "SELECT COUNT(*) FROM Admin WHERE userId = :userId", Long.class)
                .setParameter("userId", userId)
                .uniqueResult();
        Long comments = persist.getQuerySession().createQuery(
                        "SELECT COUNT(*) FROM Comment WHERE userId = :userId", Long.class)
                .setParameter("userId", userId)
                .uniqueResult();
        Long notifications = persist.getQuerySession().createQuery(
                        "SELECT COUNT(*) FROM Notification WHERE userId = :userId", Long.class)
                .setParameter("userId", userId)
                .uniqueResult();

        return otherClients > 0 || admins > 0 || comments > 0 || notifications > 0;
    }

    public void delete(User user) {
        persist.remove(user);
    }
}
