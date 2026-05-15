package com.otzar.sscm.repository;

import com.otzar.sscm.entities.Comment;
import com.otzar.sscm.service.Persist;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class CommentRepository {

    private final Persist persist;

    public CommentRepository(Persist persist) {
        this.persist = persist;
    }

    public Comment save(Comment comment) {
        persist.save(comment);
        return comment;
    }

    public List<Comment> findAll() {
        return persist.loadList(Comment.class);
    }

    public List<Comment> getCommentsByContentId(Long contentId) {
        return persist.loadListByParameter("FROM Comment WHERE contentId = :contentId", "contentId", contentId, Comment.class);
    }
}
