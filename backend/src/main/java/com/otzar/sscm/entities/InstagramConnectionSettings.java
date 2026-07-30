package com.otzar.sscm.entities;

import java.time.LocalDateTime;

public class InstagramConnectionSettings {
    private Long settingsId;
    private String instagramUserId;
    private String graphApiBaseUrl;
    private LocalDateTime updatedAt;

    public Long getSettingsId() { return settingsId; }
    public void setSettingsId(Long settingsId) { this.settingsId = settingsId; }
    public String getInstagramUserId() { return instagramUserId; }
    public void setInstagramUserId(String instagramUserId) { this.instagramUserId = instagramUserId; }
    public String getGraphApiBaseUrl() { return graphApiBaseUrl; }
    public void setGraphApiBaseUrl(String graphApiBaseUrl) { this.graphApiBaseUrl = graphApiBaseUrl; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
