package com.otzar.sscm.repository;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.service.Persist;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.hibernate.LockMode;

@Repository
@Transactional
public class ContentRepository {

    private final Persist persist;

    public ContentRepository(Persist persist) {
        this.persist = persist;
    }

    public List<Content> findAll() {
        return persist.getQuerySession()
                .createQuery("FROM Content ORDER BY createdAt DESC, content_id DESC", Content.class)
                .list();
    }

    public List<Content> findByClientId(Long clientId) {
        return persist.loadListByParameter("FROM Content WHERE clientId = :clientId ORDER BY createdAt DESC, content_id DESC", "clientId", clientId, Content.class);
    }

    public List<Content> findByStatus(ContentStatus status) {
        return persist.loadListByParameter("FROM Content WHERE status = :status ORDER BY createdAt DESC, content_id DESC", "status", status, Content.class);
    }

    public List<Content> findByClientIdAndStatus(Long clientId, ContentStatus status) {
        return persist.getQuerySession()
                .createQuery("FROM Content WHERE clientId = :clientId AND status = :status ORDER BY createdAt DESC, content_id DESC", Content.class)
                .setParameter("clientId", clientId)
                .setParameter("status", status)
                .list();
    }

    public Optional<Content> findById(Long contentId) {
        return Optional.ofNullable(persist.loadObject(Content.class, contentId));
    }

    public Optional<Content> findByIdForUpdate(Long contentId) {
        return Optional.ofNullable(persist.getQuerySession().get(Content.class, contentId, LockMode.PESSIMISTIC_WRITE));
    }

    public List<Content> findEligibleForPublishing(LocalDateTime now, int limit) {
        return persist.getQuerySession().createQuery(
                "FROM Content WHERE status=:status AND plannedPublishDate IS NOT NULL " +
                "AND plannedPublishDate<=:now ORDER BY plannedPublishDate, content_id", Content.class)
                .setParameter("status", ContentStatus.APPROVED)
                .setParameter("now", now)
                .setMaxResults(limit)
                .list();
    }

    public Content save(Content content) {
        persist.save(content);
        return content;
    }

    public void delete(Content content) {
        persist.remove(content);
    }
}
