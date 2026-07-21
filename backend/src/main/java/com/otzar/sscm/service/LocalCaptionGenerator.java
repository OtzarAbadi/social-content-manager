package com.otzar.sscm.service;

import com.otzar.sscm.models.CaptionSuggestionRequest;
import com.otzar.sscm.models.CaptionTone;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LocalCaptionGenerator implements CaptionGenerator {
    private static final int MAX_OUTPUT_LENGTH = 500;

    @Override
    public String generate(CaptionSuggestionRequest request) {
        String title = request.getTitle().trim();
        CaptionTone tone = request.getTone() == null ? CaptionTone.FRIENDLY : request.getTone();
        String caption;

        switch (tone) {
            case PROFESSIONAL:
                caption = title + " — תוכן מקצועי, ברור וממוקד שנוצר במיוחד עבור הקהל שלנו.";
                break;
            case PROMOTIONAL:
                caption = title + " ✨ זה הזמן להכיר, להתעניין ולגלות עוד.";
                break;
            case FRIENDLY:
            default:
                caption = title + " 😊 נשמח לשתף אתכם ולשמוע מה דעתכם.";
                break;
        }

        String hashtags = sanitizedHashtags(request.getKeywords());
        String result = hashtags.isEmpty() ? caption : caption + "\n" + hashtags;
        return result.length() <= MAX_OUTPUT_LENGTH ? result : result.substring(0, MAX_OUTPUT_LENGTH).trim();
    }

    private String sanitizedHashtags(List<String> keywords) {
        return (keywords == null ? Collections.<String>emptyList() : keywords).stream()
                .map(String::trim)
                .map(keyword -> keyword.replaceAll("\\s+", "_"))
                .map(keyword -> keyword.replaceAll("[^\\p{L}\\p{N}_]", ""))
                .filter(keyword -> !keyword.isEmpty())
                .distinct()
                .map(keyword -> "#" + keyword)
                .collect(Collectors.joining(" "));
    }
}
