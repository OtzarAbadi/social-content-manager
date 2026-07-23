package com.otzar.sscm.models;

import java.time.LocalDateTime;

public class PublishingResult {
    private final PublishingProviderType providerType;
    private final boolean success;
    private final String externalPostId;
    private final LocalDateTime publishedAt;
    private final String errorCode;
    private final String errorMessage;

    private PublishingResult(PublishingProviderType providerType, boolean success,
                             String externalPostId, LocalDateTime publishedAt,
                             String errorCode, String errorMessage) {
        this.providerType = providerType;
        this.success = success;
        this.externalPostId = externalPostId;
        this.publishedAt = publishedAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static PublishingResult success(PublishingProviderType providerType,
                                           String externalPostId,
                                           LocalDateTime publishedAt) {
        return new PublishingResult(providerType, true, externalPostId, publishedAt, null, null);
    }

    public static PublishingResult failure(PublishingProviderType providerType,
                                           String errorCode,
                                           String errorMessage) {
        return new PublishingResult(providerType, false, null, null, errorCode, errorMessage);
    }

    public PublishingProviderType getProviderType() { return providerType; }
    public boolean isSuccess() { return success; }
    public String getExternalPostId() { return externalPostId; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
}
