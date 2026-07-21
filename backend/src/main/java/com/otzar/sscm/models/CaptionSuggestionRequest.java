package com.otzar.sscm.models;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class CaptionSuggestionRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    private CaptionContentType contentType;
    private CaptionTone tone;

    @Size(max = 5, message = "At most 5 keywords are allowed")
    private List<@NotBlank(message = "Keyword must not be blank")
            @Size(max = 30, message = "Each keyword must be at most 30 characters") String> keywords;

    @NotNull(message = "Language is required")
    private CaptionLanguage language;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public CaptionContentType getContentType() { return contentType; }
    public void setContentType(CaptionContentType contentType) { this.contentType = contentType; }
    public CaptionTone getTone() { return tone; }
    public void setTone(CaptionTone tone) { this.tone = tone; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public CaptionLanguage getLanguage() { return language; }
    public void setLanguage(CaptionLanguage language) { this.language = language; }
}
