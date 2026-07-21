package com.otzar.sscm.models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AnalyticsDashboardResponse {
    private final LocalDateTime generatedAt;
    private final Long scopeClientId;
    private final long totalContents;
    private final long scheduledContents;
    private final long waitingApprovalContents;
    private final long publishedContents;
    private final long totalComments;
    private final double averageCommentsPerContent;
    private final Map<String, Long> contentsByStatus;
    private final Map<String, Long> contentsByType;
    private final List<AnalyticsMonthCount> scheduledByMonth;
    private final List<AnalyticsClientSummary> clientSummaries;

    public AnalyticsDashboardResponse(LocalDateTime generatedAt, Long scopeClientId,
                                      long totalContents, long scheduledContents,
                                      long waitingApprovalContents, long publishedContents,
                                      long totalComments, double averageCommentsPerContent,
                                      Map<String, Long> contentsByStatus,
                                      Map<String, Long> contentsByType,
                                      List<AnalyticsMonthCount> scheduledByMonth,
                                      List<AnalyticsClientSummary> clientSummaries) {
        this.generatedAt = generatedAt;
        this.scopeClientId = scopeClientId;
        this.totalContents = totalContents;
        this.scheduledContents = scheduledContents;
        this.waitingApprovalContents = waitingApprovalContents;
        this.publishedContents = publishedContents;
        this.totalComments = totalComments;
        this.averageCommentsPerContent = averageCommentsPerContent;
        this.contentsByStatus = contentsByStatus;
        this.contentsByType = contentsByType;
        this.scheduledByMonth = scheduledByMonth;
        this.clientSummaries = clientSummaries;
    }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public Long getScopeClientId() { return scopeClientId; }
    public long getTotalContents() { return totalContents; }
    public long getScheduledContents() { return scheduledContents; }
    public long getWaitingApprovalContents() { return waitingApprovalContents; }
    public long getPublishedContents() { return publishedContents; }
    public long getTotalComments() { return totalComments; }
    public double getAverageCommentsPerContent() { return averageCommentsPerContent; }
    public Map<String, Long> getContentsByStatus() { return contentsByStatus; }
    public Map<String, Long> getContentsByType() { return contentsByType; }
    public List<AnalyticsMonthCount> getScheduledByMonth() { return scheduledByMonth; }
    public List<AnalyticsClientSummary> getClientSummaries() { return clientSummaries; }
}
