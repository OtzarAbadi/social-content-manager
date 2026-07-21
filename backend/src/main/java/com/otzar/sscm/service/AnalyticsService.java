package com.otzar.sscm.service;

import com.otzar.sscm.entities.Client;
import com.otzar.sscm.entities.Comment;
import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.models.AnalyticsClientSummary;
import com.otzar.sscm.models.AnalyticsDashboardResponse;
import com.otzar.sscm.models.AnalyticsMonthCount;
import com.otzar.sscm.repository.AnalyticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private static final List<String> CONTENT_TYPES = Arrays.asList("IMAGE", "VIDEO", "TEXT");

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsDashboardResponse getAdminDashboard() {
        List<Content> contents = analyticsRepository.findContents(null);
        List<Comment> comments = analyticsRepository.findComments(null);
        List<AnalyticsClientSummary> clientSummaries = buildClientSummaries(
                analyticsRepository.findClients(), contents, comments);
        return buildResponse(null, contents, comments, clientSummaries);
    }

    @Transactional(readOnly = true)
    public AnalyticsDashboardResponse getClientDashboard(Long clientId) {
        List<Content> contents = analyticsRepository.findContents(clientId);
        List<Comment> comments = analyticsRepository.findComments(clientId);
        return buildResponse(clientId, contents, comments, Collections.emptyList());
    }

    private AnalyticsDashboardResponse buildResponse(Long scopeClientId, List<Content> contents,
                                                     List<Comment> comments,
                                                     List<AnalyticsClientSummary> clientSummaries) {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ContentStatus status : ContentStatus.values()) {
            byStatus.put(status.name(), 0L);
        }

        Map<String, Long> byType = new LinkedHashMap<>();
        CONTENT_TYPES.forEach(type -> byType.put(type, 0L));
        Map<String, Long> byMonth = new TreeMap<>();

        long scheduled = 0;
        for (Content content : contents) {
            if (content.getStatus() != null) {
                byStatus.compute(content.getStatus().name(), (key, count) -> count == null ? 1L : count + 1);
            }
            if (content.getContent_type() != null) {
                byType.compute(content.getContent_type(), (key, count) -> count == null ? 1L : count + 1);
            }
            if (content.getPlannedPublishDate() != null) {
                scheduled++;
                String month = YearMonth.from(content.getPlannedPublishDate()).toString();
                byMonth.merge(month, 1L, Long::sum);
            }
        }

        List<AnalyticsMonthCount> scheduledByMonth = byMonth.entrySet().stream()
                .map(entry -> new AnalyticsMonthCount(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        long totalContents = contents.size();
        long totalComments = comments.size();
        double average = totalContents == 0 ? 0.0 : (double) totalComments / totalContents;

        return new AnalyticsDashboardResponse(
                LocalDateTime.now(), scopeClientId, totalContents, scheduled,
                byStatus.get(ContentStatus.WAITING_APPROVAL.name()),
                byStatus.get(ContentStatus.PUBLISHED.name()), totalComments, average,
                byStatus, byType, scheduledByMonth, clientSummaries);
    }

    private List<AnalyticsClientSummary> buildClientSummaries(List<Client> clients,
                                                               List<Content> contents,
                                                               List<Comment> comments) {
        Map<Long, List<Content>> contentsByClient = contents.stream()
                .collect(Collectors.groupingBy(Content::getClientId));
        Map<Long, Long> clientByContent = contents.stream()
                .collect(Collectors.toMap(Content::getContent_id, Content::getClientId, (first, second) -> first));
        Map<Long, Long> commentsByClient = comments.stream()
                .map(Comment::getContentId)
                .map(clientByContent::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<AnalyticsClientSummary> summaries = new ArrayList<>();
        for (Client client : clients) {
            List<Content> clientContents = contentsByClient.getOrDefault(client.getClient_id(), Collections.emptyList());
            long scheduled = clientContents.stream().filter(item -> item.getPlannedPublishDate() != null).count();
            long waiting = clientContents.stream()
                    .filter(item -> item.getStatus() == ContentStatus.WAITING_APPROVAL).count();
            long published = clientContents.stream()
                    .filter(item -> item.getStatus() == ContentStatus.PUBLISHED).count();
            summaries.add(new AnalyticsClientSummary(
                    client.getClient_id(), client.getBusiness_name(), clientContents.size(), scheduled,
                    waiting, published, commentsByClient.getOrDefault(client.getClient_id(), 0L)));
        }
        return summaries;
    }
}
