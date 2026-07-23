package com.otzar.sscm.entities;

import com.otzar.sscm.models.PublishingProviderType;
import java.time.LocalDateTime;

public class PublicationRecord {
    private Long publicationId;
    private String deliveryKey;
    private Long contentId;
    private PublishingProviderType provider;
    private String targetPlatform;
    private PublicationStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime publishedAt;
    private String externalPostId;
    private String errorCode;
    private String errorMessage;
    private Integer attemptNumber;
    private PublicationTriggerType triggerType;
    private Long requestedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getPublicationId() { return publicationId; }
    public void setPublicationId(Long value) { publicationId = value; }
    public String getDeliveryKey() { return deliveryKey; }
    public void setDeliveryKey(String value) { deliveryKey = value; }
    public Long getContentId() { return contentId; }
    public void setContentId(Long value) { contentId = value; }
    public PublishingProviderType getProvider() { return provider; }
    public void setProvider(PublishingProviderType value) { provider = value; }
    public String getTargetPlatform() { return targetPlatform; }
    public void setTargetPlatform(String value) { targetPlatform = value; }
    public PublicationStatus getStatus() { return status; }
    public void setStatus(PublicationStatus value) { status = value; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime value) { requestedAt = value; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime value) { startedAt = value; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime value) { publishedAt = value; }
    public String getExternalPostId() { return externalPostId; }
    public void setExternalPostId(String value) { externalPostId = value; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String value) { errorCode = value; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String value) { errorMessage = value; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer value) { attemptNumber = value; }
    public PublicationTriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(PublicationTriggerType value) { triggerType = value; }
    public Long getRequestedByUserId() { return requestedByUserId; }
    public void setRequestedByUserId(Long value) { requestedByUserId = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
}
