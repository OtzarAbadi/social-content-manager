package com.otzar.sscm.service;

import com.otzar.sscm.models.PublishingProviderType;
import com.otzar.sscm.models.PublishingRequest;
import com.otzar.sscm.models.PublishingResult;

public interface PublishingProvider {
    PublishingProviderType getType();
    PublishingResult publish(PublishingRequest request);
}
