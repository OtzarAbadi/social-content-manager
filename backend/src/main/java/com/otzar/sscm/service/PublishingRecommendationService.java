package com.otzar.sscm.service;

import com.otzar.sscm.models.PublishingRecommendationRequest;
import com.otzar.sscm.models.PublishingRecommendationResponse;
import org.springframework.stereotype.Service;

@Service
public class PublishingRecommendationService {
    private final PublishingRecommendationGenerator generator;

    public PublishingRecommendationService(PublishingRecommendationGenerator generator) {
        this.generator = generator;
    }

    public PublishingRecommendationResponse recommend(PublishingRecommendationRequest request) {
        return generator.generate(request);
    }
}
