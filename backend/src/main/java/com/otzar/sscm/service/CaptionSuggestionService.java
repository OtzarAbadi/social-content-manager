package com.otzar.sscm.service;

import com.otzar.sscm.models.CaptionSuggestionRequest;
import com.otzar.sscm.models.CaptionSuggestionResponse;
import org.springframework.stereotype.Service;

@Service
public class CaptionSuggestionService {
    private final CaptionGenerator captionGenerator;

    public CaptionSuggestionService(CaptionGenerator captionGenerator) {
        this.captionGenerator = captionGenerator;
    }

    public CaptionSuggestionResponse suggest(CaptionSuggestionRequest request) {
        return new CaptionSuggestionResponse(
                captionGenerator.generate(request), "LOCAL_SIMULATOR", true);
    }
}
