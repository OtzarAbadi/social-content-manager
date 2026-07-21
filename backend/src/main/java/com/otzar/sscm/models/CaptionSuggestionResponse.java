package com.otzar.sscm.models;

public class CaptionSuggestionResponse {
    private final String caption;
    private final String provider;
    private final boolean generated;

    public CaptionSuggestionResponse(String caption, String provider, boolean generated) {
        this.caption = caption;
        this.provider = provider;
        this.generated = generated;
    }

    public String getCaption() { return caption; }
    public String getProvider() { return provider; }
    public boolean isGenerated() { return generated; }
}
