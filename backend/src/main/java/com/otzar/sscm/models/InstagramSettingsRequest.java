package com.otzar.sscm.models;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class InstagramSettingsRequest {
    @NotBlank
    @Pattern(regexp = "^[0-9]{5,40}$", message = "Instagram account ID must contain digits only")
    private String instagramUserId;

    @NotBlank
    @Pattern(regexp = "^https://graph\\.facebook\\.com/v[0-9]+\\.[0-9]+/?$",
            message = "Graph API URL must use graph.facebook.com and include a version")
    private String graphApiBaseUrl;

    public String getInstagramUserId() { return instagramUserId; }
    public void setInstagramUserId(String value) { instagramUserId = value; }
    public String getGraphApiBaseUrl() { return graphApiBaseUrl; }
    public void setGraphApiBaseUrl(String value) { graphApiBaseUrl = value; }
}
