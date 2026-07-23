package com.otzar.sscm;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.entities.PublicationTriggerType;
import com.otzar.sscm.repository.ContentRepository;
import com.otzar.sscm.service.AutomaticPublishingScheduler;
import com.otzar.sscm.service.SocialPublishingService;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.mockito.Mockito.*;

class AutomaticPublishingSchedulerTests {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 23, 12, 0);
    private static final Clock CLOCK =
            Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Test
    void publishesOnlyEligibleContentWithScheduledTrigger() {
        ContentRepository repository = mock(ContentRepository.class);
        SocialPublishingService publishing = mock(SocialPublishingService.class);
        Content eligible = content(1L, ContentStatus.APPROVED, NOW);
        Content future = content(2L, ContentStatus.APPROVED, NOW.plusSeconds(1));
        Content missingDate = content(3L, ContentStatus.APPROVED, null);
        Content draft = content(4L, ContentStatus.DRAFT, NOW);
        when(repository.findEligibleForPublishing(NOW, 25))
                .thenReturn(List.of(eligible, future, missingDate, draft));

        new AutomaticPublishingScheduler(repository, publishing, CLOCK, true, 25).poll();

        verify(publishing).publish(1L, null, PublicationTriggerType.SCHEDULED);
        verify(publishing, never()).publish(eq(2L), any(), any());
        verify(publishing, never()).publish(eq(3L), any(), any());
        verify(publishing, never()).publish(eq(4L), any(), any());
    }

    @Test
    void disabledSchedulerDoesNothing() {
        ContentRepository repository = mock(ContentRepository.class);
        SocialPublishingService publishing = mock(SocialPublishingService.class);
        new AutomaticPublishingScheduler(repository, publishing, CLOCK, false, 25).poll();
        verifyNoInteractions(repository, publishing);
    }

    private Content content(Long id, ContentStatus status, LocalDateTime plannedDate) {
        Content content = new Content();
        content.setContent_id(id);
        content.setStatus(status);
        content.setPlannedPublishDate(plannedDate);
        return content;
    }
}
