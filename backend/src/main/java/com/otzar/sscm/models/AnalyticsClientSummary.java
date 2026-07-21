package com.otzar.sscm.models;

public class AnalyticsClientSummary {
    private final Long clientId;
    private final String businessName;
    private final long totalContents;
    private final long scheduledContents;
    private final long waitingApprovalContents;
    private final long publishedContents;
    private final long commentCount;

    public AnalyticsClientSummary(Long clientId, String businessName, long totalContents,
                                  long scheduledContents, long waitingApprovalContents,
                                  long publishedContents, long commentCount) {
        this.clientId = clientId;
        this.businessName = businessName;
        this.totalContents = totalContents;
        this.scheduledContents = scheduledContents;
        this.waitingApprovalContents = waitingApprovalContents;
        this.publishedContents = publishedContents;
        this.commentCount = commentCount;
    }

    public Long getClientId() { return clientId; }
    public String getBusinessName() { return businessName; }
    public long getTotalContents() { return totalContents; }
    public long getScheduledContents() { return scheduledContents; }
    public long getWaitingApprovalContents() { return waitingApprovalContents; }
    public long getPublishedContents() { return publishedContents; }
    public long getCommentCount() { return commentCount; }
}
