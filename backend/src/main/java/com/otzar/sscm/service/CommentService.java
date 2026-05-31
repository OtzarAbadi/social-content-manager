package com.otzar.sscm.service;

import com.otzar.sscm.entities.Comment;
import com.otzar.sscm.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public Comment create(Comment comment) {
        return commentRepository.save(comment);
    }

    public List<Comment> findAll() {
        return commentRepository.findAll();
    }

    public List<Comment> findByContentId(Long contentId) {
        return commentRepository.getCommentsByContentId(contentId);
    }
}
