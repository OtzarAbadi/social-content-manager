package com.otzar.sscm.models;

import java.time.LocalDateTime;

public class PublishingRequest {
    private final String requestId;
    private final Long contentId;
    private final Long clientId;
    private final String title;
    private final String description;
    private final String contentType;
    private final String fileUrl;
    private final LocalDateTime plannedPublishDate;

    public PublishingRequest(String requestId, Long contentId, Long clientId, String title,
                             String description, String contentType, String fileUrl,
                             LocalDateTime plannedPublishDate) {
        this.requestId = requestId;
        this.contentId = contentId;
        this.clientId = clientId;
        this.title = title;
        this.description = description;
        this.contentType = contentType;
        this.fileUrl = fileUrl;
        this.plannedPublishDate = plannedPublishDate;
    }

    public String getRequestId() { return requestId; }
    public Long getContentId() { return contentId; }
    public Long getClientId() { return clientId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getContentType() { return contentType; }
    public String getFileUrl() { return fileUrl; }
    public LocalDateTime getPlannedPublishDate() { return plannedPublishDate; }
}
