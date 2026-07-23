package com.otzar.sscm.models;

import com.otzar.sscm.entities.ContentStatus;

import java.time.LocalDateTime;

public class ActivityResponse {
    private final String activityId;
    private final ActivitySource source;
    private final ActivityType type;
    private final LocalDateTime occurredAt;
    private final Long contentId;
    private final String contentTitle;
    private final Long clientId;
    private final String clientName;
    private final ContentStatus status;
    private final Integer versionNumber;

    public ActivityResponse(String activityId, ActivitySource source, ActivityType type,
                            LocalDateTime occurredAt, Long contentId, String contentTitle,
                            Long clientId, String clientName, ContentStatus status,
                            Integer versionNumber) {
        this.activityId = activityId;
        this.source = source;
        this.type = type;
        this.occurredAt = occurredAt;
        this.contentId = contentId;
        this.contentTitle = contentTitle;
        this.clientId = clientId;
        this.clientName = clientName;
        this.status = status;
        this.versionNumber = versionNumber;
    }

    public String getActivityId() { return activityId; }
    public ActivitySource getSource() { return source; }
    public ActivityType getType() { return type; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public Long getContentId() { return contentId; }
    public String getContentTitle() { return contentTitle; }
    public Long getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public ContentStatus getStatus() { return status; }
    public Integer getVersionNumber() { return versionNumber; }
}
