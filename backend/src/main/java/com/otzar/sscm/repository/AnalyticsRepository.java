package com.otzar.sscm.repository;

import com.otzar.sscm.entities.Client;
import com.otzar.sscm.entities.Comment;
import com.otzar.sscm.entities.Content;
import com.otzar.sscm.service.Persist;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public class AnalyticsRepository {
    private final Persist persist;

    public AnalyticsRepository(Persist persist) {
        this.persist = persist;
    }

    public List<Content> findContents(Long clientId) {
        if (clientId == null) {
            return persist.loadList(Content.class);
        }
        return persist.loadListByParameter(
                "FROM Content WHERE clientId = :clientId", "clientId", clientId, Content.class);
    }

    public List<Comment> findComments(Long clientId) {
        if (clientId == null) {
            return persist.loadList(Comment.class);
        }
        return persist.getQuerySession().createQuery(
                        "FROM Comment WHERE contentId IN " +
                                "(SELECT content_id FROM Content WHERE clientId = :clientId)", Comment.class)
                .setParameter("clientId", clientId)
                .list();
    }

    public List<Client> findClients() {
        return persist.getQuerySession()
                .createQuery("FROM Client WHERE archived = false ORDER BY client_id", Client.class)
                .list();
    }
}
