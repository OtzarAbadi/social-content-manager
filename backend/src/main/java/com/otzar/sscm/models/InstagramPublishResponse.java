package com.otzar.sscm.models;

public class InstagramPublishResponse {
    private final boolean success;
    private final String instagramMediaId;

    public InstagramPublishResponse(boolean success, String instagramMediaId) {
        this.success = success;
        this.instagramMediaId = instagramMediaId;
    }

    public boolean isSuccess() { return success; }
    public String getInstagramMediaId() { return instagramMediaId; }
}
