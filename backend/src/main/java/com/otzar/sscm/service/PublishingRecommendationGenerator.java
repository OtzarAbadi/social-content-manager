package com.otzar.sscm.service;

import com.otzar.sscm.models.PublishingRecommendationRequest;
import com.otzar.sscm.models.PublishingRecommendationResponse;

public interface PublishingRecommendationGenerator {
    PublishingRecommendationResponse generate(PublishingRecommendationRequest request);
}
