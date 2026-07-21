package com.otzar.sscm.service;

import com.otzar.sscm.models.CaptionSuggestionRequest;

public interface CaptionGenerator {
    String generate(CaptionSuggestionRequest request);
}
