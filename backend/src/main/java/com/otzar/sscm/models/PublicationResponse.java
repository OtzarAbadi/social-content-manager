package com.otzar.sscm.models;

import com.otzar.sscm.entities.PublicationRecord;
import com.otzar.sscm.entities.PublicationStatus;
import com.otzar.sscm.entities.PublicationTriggerType;
import java.time.LocalDateTime;

public class PublicationResponse {
    public final Long publicationId;
    public final String deliveryKey;
    public final Long contentId;
    public final PublishingProviderType provider;
    public final String targetPlatform;
    public final PublicationStatus status;
    public final LocalDateTime requestedAt;
    public final LocalDateTime startedAt;
    public final LocalDateTime publishedAt;
    public final String externalPostId;
    public final String errorCode;
    public final String errorMessage;
    public final Integer attemptNumber;
    public final PublicationTriggerType triggerType;
    public final Long requestedByUserId;
    public final LocalDateTime createdAt;
    public final LocalDateTime updatedAt;

    public PublicationResponse(PublicationRecord r) {
        publicationId = r.getPublicationId(); deliveryKey = r.getDeliveryKey();
        contentId = r.getContentId(); provider = r.getProvider();
        targetPlatform = r.getTargetPlatform(); status = r.getStatus();
        requestedAt = r.getRequestedAt(); startedAt = r.getStartedAt();
        publishedAt = r.getPublishedAt(); externalPostId = r.getExternalPostId();
        errorCode = r.getErrorCode(); errorMessage = r.getErrorMessage();
        attemptNumber = r.getAttemptNumber(); triggerType = r.getTriggerType();
        requestedByUserId = r.getRequestedByUserId(); createdAt = r.getCreatedAt();
        updatedAt = r.getUpdatedAt();
    }
}
