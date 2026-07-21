package com.otzar.sscm.entities;

import java.time.LocalDateTime;

public class ContentVersion {

    private Long contentVersionId;
    private Long contentId;
    private Integer versionNumber;
    private String title;
    private String description;
    private String contentType;
    private String fileUrl;
    private ContentStatus status;
    private LocalDateTime plannedPublishDate;
    private Long changedByUserId;
    private LocalDateTime changedAt;
    private ContentVersionChangeType changeType;

    public Long getContentVersionId() { return contentVersionId; }
    public void setContentVersionId(Long contentVersionId) { this.contentVersionId = contentVersionId; }
    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }
    public LocalDateTime getPlannedPublishDate() { return plannedPublishDate; }
    public void setPlannedPublishDate(LocalDateTime plannedPublishDate) { this.plannedPublishDate = plannedPublishDate; }
    public Long getChangedByUserId() { return changedByUserId; }
    public void setChangedByUserId(Long changedByUserId) { this.changedByUserId = changedByUserId; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    public ContentVersionChangeType getChangeType() { return changeType; }
    public void setChangeType(ContentVersionChangeType changeType) { this.changeType = changeType; }
}
