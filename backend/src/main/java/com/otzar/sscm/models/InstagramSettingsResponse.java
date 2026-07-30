package com.otzar.sscm.models;

import java.time.LocalDateTime;

public class InstagramSettingsResponse {
    public final String instagramUserId;
    public final String graphApiBaseUrl;
    public final boolean accessTokenConfigured;
    public final LocalDateTime updatedAt;
    public final String instagramUserIdSource;
    public final String graphApiBaseUrlSource;

    public InstagramSettingsResponse(String instagramUserId, String graphApiBaseUrl,
                                     boolean accessTokenConfigured, LocalDateTime updatedAt,
                                     String instagramUserIdSource, String graphApiBaseUrlSource) {
        this.instagramUserId = instagramUserId;
        this.graphApiBaseUrl = graphApiBaseUrl;
        this.accessTokenConfigured = accessTokenConfigured;
        this.updatedAt = updatedAt;
        this.instagramUserIdSource = instagramUserIdSource;
        this.graphApiBaseUrlSource = graphApiBaseUrlSource;
    }
}
