package com.otzar.sscm.repository;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.service.Persist;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class ContentRepository {

    private final Persist persist;

    public ContentRepository(Persist persist) {
        this.persist = persist;
    }

    public List<Content> findAll() {
        return persist.loadList(Content.class);
    }

    public List<Content> findByClientId(Long clientId) {
        return persist.loadListByParameter("FROM Content WHERE clientId = :clientId", "clientId", clientId, Content.class);
    }

    public Optional<Content> findById(Long contentId) {
        return Optional.ofNullable(persist.loadObject(Content.class, contentId));
    }

    public Content save(Content content) {
        persist.save(content);
        return content;
    }
}
