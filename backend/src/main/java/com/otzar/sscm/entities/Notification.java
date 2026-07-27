package com.otzar.sscm.entities;

import java.time.LocalDateTime;

public class Notification {
    private Long notificationId;
    private Long userId;
    private NotificationType type;
    private String title;
    private String message;
    private Long relatedContentId;
    private Long entityId;
    private boolean read;
    private LocalDateTime createdAt;

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getRelatedContentId() { return relatedContentId; }
    public void setRelatedContentId(Long relatedContentId) { this.relatedContentId = relatedContentId; }
    public Long getEntityId() { return entityId != null ? entityId : relatedContentId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
