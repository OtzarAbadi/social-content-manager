package com.otzar.sscm.models;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public class PublishingRecommendationRequest {
    @NotNull(message = "Content type is required")
    private PublishingRecommendationContentType contentType;

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @Size(max = 5, message = "At most 5 keywords are allowed")
    private List<@NotBlank(message = "Keyword must not be blank")
            @Size(max = 30, message = "Each keyword must be at most 30 characters") String> keywords;

    @NotNull(message = "Client ID is required")
    @Positive(message = "Client ID must be positive")
    private Long clientId;

    private LocalDateTime existingPlannedPublishDate;

    public PublishingRecommendationContentType getContentType() { return contentType; }
    public void setContentType(PublishingRecommendationContentType contentType) { this.contentType = contentType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public LocalDateTime getExistingPlannedPublishDate() { return existingPlannedPublishDate; }
    public void setExistingPlannedPublishDate(LocalDateTime existingPlannedPublishDate) {
        this.existingPlannedPublishDate = existingPlannedPublishDate;
    }
}
