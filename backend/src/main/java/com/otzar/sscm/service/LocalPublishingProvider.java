package com.otzar.sscm.service;

import com.otzar.sscm.models.PublishingProviderType;
import com.otzar.sscm.models.PublishingRequest;
import com.otzar.sscm.models.PublishingResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LocalPublishingProvider implements PublishingProvider {
    @Override
    public PublishingProviderType getType() {
        return PublishingProviderType.LOCAL;
    }

    @Override
    public PublishingResult publish(PublishingRequest request) {
        return PublishingResult.success(PublishingProviderType.LOCAL, null, LocalDateTime.now());
    }
}
