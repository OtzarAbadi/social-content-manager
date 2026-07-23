package com.otzar.sscm.service;

import com.otzar.sscm.models.PublishingProviderType;
import com.otzar.sscm.models.PublishingRequest;
import com.otzar.sscm.models.PublishingResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Component
public class LocalPublishingProvider implements PublishingProvider {
    private final Clock clock;

    public LocalPublishingProvider() { this(Clock.systemDefaultZone()); }

    @Autowired
    public LocalPublishingProvider(@Qualifier("publishingRecommendationClock") Clock clock) { this.clock = clock; }
    @Override
    public PublishingProviderType getType() {
        return PublishingProviderType.LOCAL;
    }

    @Override
    public PublishingResult publish(PublishingRequest request) {
        return PublishingResult.success(PublishingProviderType.LOCAL, null, LocalDateTime.now(clock));
    }
}
