package com.otzar.sscm.service;

import com.otzar.sscm.entities.*;
import com.otzar.sscm.models.PublishingProviderType;
import com.otzar.sscm.models.PublishingResult;
import com.otzar.sscm.repository.ContentRepository;
import com.otzar.sscm.repository.PublicationRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PublicationTransactionService {
    private final ContentRepository contents;
    private final PublicationRecordRepository publications;
    private final ContentService contentService;
    private final NotificationService notifications;
    private final Clock clock;

    public PublicationTransactionService(ContentRepository contents, PublicationRecordRepository publications,
            ContentService contentService, NotificationService notifications,
            @Qualifier("publishingRecommendationClock") Clock clock) {
        this.contents = contents; this.publications = publications; this.contentService = contentService;
        this.notifications = notifications; this.clock = clock;
    }

    @Transactional
    public Optional<Claim> claim(Long contentId, Long userId, PublicationTriggerType trigger,
                                 PublishingProviderType provider) {
        Optional<Content> found = contents.findByIdForUpdate(contentId);
        if (found.isEmpty()) return Optional.empty();
        Content content = found.get();
        if (content.getStatus() != ContentStatus.APPROVED) {
            throw new IllegalStateException("Only approved content can be published");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        PublicationRecord record = new PublicationRecord();
        record.setDeliveryKey("content:" + contentId);
        record.setContentId(contentId);
        record.setProvider(provider);
        record.setStatus(PublicationStatus.PROCESSING);
        record.setRequestedAt(now); record.setStartedAt(now);
        record.setAttemptNumber(1); record.setTriggerType(trigger);
        record.setRequestedByUserId(userId); record.setCreatedAt(now); record.setUpdatedAt(now);
        try {
            publications.save(record);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Content publication is already in progress or complete");
        }
        return Optional.of(new Claim(content, record.getPublicationId(), record.getDeliveryKey()));
    }

    @Transactional
    public Optional<Content> succeed(Long publicationId, Long changedByUserId, PublishingResult result) {
        PublicationRecord record = publications.findByIdForUpdate(publicationId)
                .orElseThrow(() -> new IllegalStateException("Publication record not found"));
        if (record.getStatus() == PublicationStatus.SUCCEEDED) return contentService.findById(record.getContentId());
        if (record.getStatus() != PublicationStatus.PROCESSING) throw new IllegalStateException("Publication is not active");
        Content content = contentService.publish(record.getContentId(), changedByUserId)
                .orElseThrow(() -> new IllegalStateException("Content not found"));
        LocalDateTime now = LocalDateTime.now(clock);
        record.setStatus(PublicationStatus.SUCCEEDED);
        record.setPublishedAt(result.getPublishedAt() == null ? now : result.getPublishedAt());
        record.setExternalPostId(limit(result.getExternalPostId(), 255));
        record.setUpdatedAt(now);
        publications.save(record);
        notifications.notifyClient(content, NotificationType.CONTENT_PUBLISHED,
                "התוכן פורסם", "התוכן '" + content.getTitle() + "' פורסם");
        return Optional.of(content);
    }

    @Transactional
    public void fail(Long publicationId, PublishingResult result, RuntimeException cause) {
        PublicationRecord record = publications.findByIdForUpdate(publicationId).orElse(null);
        if (record == null || record.getStatus() != PublicationStatus.PROCESSING) return;
        record.setStatus(PublicationStatus.FAILED);
        record.setErrorCode(limit(result == null ? "PROVIDER_ERROR" : result.getErrorCode(), 100));
        String message = result == null ? (cause == null ? "Publishing provider failed" : cause.getMessage())
                : result.getErrorMessage();
        record.setErrorMessage(limit(message == null ? "Publishing provider failed" : message, 1000));
        record.setUpdatedAt(LocalDateTime.now(clock));
        publications.save(record);
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        String clean = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    public static class Claim {
        public final Content content; public final Long publicationId; public final String deliveryKey;
        Claim(Content content, Long publicationId, String deliveryKey) {
            this.content = content; this.publicationId = publicationId; this.deliveryKey = deliveryKey;
        }
    }
}
