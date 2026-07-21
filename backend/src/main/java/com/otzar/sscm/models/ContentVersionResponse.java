package com.otzar.sscm.models;

import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.entities.ContentVersion;
import com.otzar.sscm.entities.ContentVersionChangeType;

import java.time.LocalDateTime;

public class ContentVersionResponse {

    private final Long contentVersionId;
    private final Long contentId;
    private final Integer versionNumber;
    private final String title;
    private final String description;
    private final String contentType;
    private final String fileUrl;
    private final ContentStatus status;
    private final LocalDateTime plannedPublishDate;
    private final Long changedByUserId;
    private final LocalDateTime changedAt;
    private final ContentVersionChangeType changeType;

    public ContentVersionResponse(ContentVersion version) {
        this.contentVersionId = version.getContentVersionId();
        this.contentId = version.getContentId();
        this.versionNumber = version.getVersionNumber();
        this.title = version.getTitle();
        this.description = version.getDescription();
        this.contentType = version.getContentType();
        this.fileUrl = version.getFileUrl();
        this.status = version.getStatus();
        this.plannedPublishDate = version.getPlannedPublishDate();
        this.changedByUserId = version.getChangedByUserId();
        this.changedAt = version.getChangedAt();
        this.changeType = version.getChangeType();
    }

    public Long getContentVersionId() { return contentVersionId; }
    public Long getContentId() { return contentId; }
    public Integer getVersionNumber() { return versionNumber; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getContentType() { return contentType; }
    public String getFileUrl() { return fileUrl; }
    public ContentStatus getStatus() { return status; }
    public LocalDateTime getPlannedPublishDate() { return plannedPublishDate; }
    public Long getChangedByUserId() { return changedByUserId; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public ContentVersionChangeType getChangeType() { return changeType; }
}
