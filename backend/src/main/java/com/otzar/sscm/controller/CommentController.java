package com.otzar.sscm.controller;

import com.otzar.sscm.entities.Comment;
import com.otzar.sscm.service.CommentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public Comment addComment(@RequestBody Comment comment) {
        return commentService.create(comment);
    }

    @GetMapping
    public List<Comment> getAllComments() {
        return commentService.findAll();
    }

    @GetMapping("/by-content")
    public List<Comment> getCommentsByContent(@RequestParam Long contentId) {
        return commentService.findByContentId(contentId);
    }
}
