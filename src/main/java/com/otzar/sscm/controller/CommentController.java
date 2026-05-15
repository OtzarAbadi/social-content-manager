package com.otzar.sscm.controller;

import com.otzar.sscm.entities.Comment;
import com.otzar.sscm.repository.CommentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentRepository commentRepository;

    public CommentController(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @PostMapping
    public Comment addComment(@RequestBody Comment comment) {
        return commentRepository.save(comment);
    }

    @GetMapping
    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }

    @GetMapping("/by-content")
    public List<Comment> getCommentsByContent(@RequestParam Long contentId) {
        return commentRepository.getCommentsByContentId(contentId);
    }
}