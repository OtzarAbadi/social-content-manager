package com.otzar.sscm.models;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class CreateCommentRequest {

    @NotNull(message = "Content ID is required")
    private Long contentId;

    @NotBlank(message = "Comment text is required")
    private String commentText;

    public Long getContentId() {
        return contentId;
    }

    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }
}
