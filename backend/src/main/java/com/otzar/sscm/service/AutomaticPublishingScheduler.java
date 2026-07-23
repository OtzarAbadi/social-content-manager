package com.otzar.sscm.service;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.entities.PublicationTriggerType;
import com.otzar.sscm.repository.ContentRepository;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.List;

@Component
public class AutomaticPublishingScheduler {
    private static final Logger log = LoggerFactory.getLogger(AutomaticPublishingScheduler.class);
    private final ContentRepository contents;
    private final SocialPublishingService publishing;
    private final Clock clock;
    private final boolean enabled;
    private final int batchSize;

    public AutomaticPublishingScheduler(ContentRepository contents, SocialPublishingService publishing,
            @Qualifier("publishingRecommendationClock") Clock clock,
            @Value("${sscm.publishing.scheduling.enabled:false}") boolean enabled,
            @Value("${sscm.publishing.scheduling.batch-size:25}") int batchSize) {
        this.contents = contents; this.publishing = publishing; this.clock = clock;
        this.enabled = enabled; this.batchSize = Math.max(1, Math.min(batchSize, 100));
    }

    @Scheduled(fixedDelayString = "${sscm.publishing.scheduling.fixed-delay-ms:60000}",
               initialDelayString = "${sscm.publishing.scheduling.initial-delay-ms:30000}")
    public void poll() {
        if (!enabled) return;
        LocalDateTime now = LocalDateTime.now(clock);
        List<Content> candidates = contents.findEligibleForPublishing(now, batchSize);
        log.info("Scheduler found {} items", candidates.size());
        for (Content content : candidates) {
            if (!isEligible(content, now)) {
                log.info("Skipping content {} - not eligible", content.getContent_id());
                continue;
            }
            try {
                log.info("Publishing content {} via scheduler", content.getContent_id());
                publishing.publish(content.getContent_id(), null, PublicationTriggerType.SCHEDULED);
            } catch (IllegalStateException ex) {
                log.info("Skipping content {} - not eligible", content.getContent_id());
            } catch (RuntimeException ex) {
                log.error("Scheduled publication failed for content {}", content.getContent_id(), ex);
            }
        }
    }

    private boolean isEligible(Content content, LocalDateTime now) {
        return content != null
                && content.getStatus() == ContentStatus.APPROVED
                && content.getPlannedPublishDate() != null
                && !content.getPlannedPublishDate().isAfter(now);
    }
}
