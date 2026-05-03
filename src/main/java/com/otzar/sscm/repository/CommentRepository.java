package com.otzar.sscm.repository;

import com.otzar.sscm.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.contentId = :contentId")
    List<Comment> getCommentsByContentId(@Param("contentId") Long contentId);
}