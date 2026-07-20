package com.otzar.sscm.service;

import com.otzar.sscm.entities.Comment;
import com.otzar.sscm.entities.User;
import com.otzar.sscm.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final AuthService authService;

    public CommentService(CommentRepository commentRepository, AuthService authService) {
        this.commentRepository = commentRepository;
        this.authService = authService;
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

    public DeleteResult delete(Long commentId, User currentUser) {
        return commentRepository.findById(commentId)
                .map(comment -> deleteIfAllowed(comment, currentUser))
                .orElse(DeleteResult.NOT_FOUND);
    }

    private DeleteResult deleteIfAllowed(Comment comment, User currentUser) {
        if (!authService.isAdmin(currentUser) && !currentUser.getUser_id().equals(comment.getUserId())) {
            return DeleteResult.FORBIDDEN;
        }

        commentRepository.delete(comment);
        return DeleteResult.DELETED;
    }

    public enum DeleteResult {
        DELETED,
        FORBIDDEN,
        NOT_FOUND
    }
}
