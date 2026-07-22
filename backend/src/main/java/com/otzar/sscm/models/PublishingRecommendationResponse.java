package com.otzar.sscm.models;

import java.time.LocalDateTime;
import java.util.List;

public class PublishingRecommendationResponse {
    private final LocalDateTime recommendedPlannedPublishDate;
    private final String timezone;
    private final String provider;
    private final boolean generated;
    private final String ruleVersion;
    private final String rationale;
    private final List<PublishingRecommendationInput> inputsUsed;

    public PublishingRecommendationResponse(LocalDateTime recommendedPlannedPublishDate, String timezone,
                                            String provider, boolean generated, String ruleVersion,
                                            String rationale, List<PublishingRecommendationInput> inputsUsed) {
        this.recommendedPlannedPublishDate = recommendedPlannedPublishDate;
        this.timezone = timezone;
        this.provider = provider;
        this.generated = generated;
        this.ruleVersion = ruleVersion;
        this.rationale = rationale;
        this.inputsUsed = List.copyOf(inputsUsed);
    }

    public LocalDateTime getRecommendedPlannedPublishDate() { return recommendedPlannedPublishDate; }
    public String getTimezone() { return timezone; }
    public String getProvider() { return provider; }
    public boolean isGenerated() { return generated; }
    public String getRuleVersion() { return ruleVersion; }
    public String getRationale() { return rationale; }
    public List<PublishingRecommendationInput> getInputsUsed() { return inputsUsed; }
}
