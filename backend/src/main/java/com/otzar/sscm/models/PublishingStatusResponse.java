package com.otzar.sscm.models;

public class PublishingStatusResponse {
    public final PublishingProviderType activeProvider;
    public final boolean externalPublishingSupported;
    public final boolean automaticPublishingEnabled;
    public final long pollingIntervalSeconds;
    public final boolean configured;

    public PublishingStatusResponse(PublishingProviderType provider, boolean enabled, long delayMs) {
        activeProvider = provider;
        externalPublishingSupported = false;
        automaticPublishingEnabled = enabled;
        pollingIntervalSeconds = Math.max(1, delayMs / 1000);
        configured = true;
    }
}
