package com.otzar.sscm.service;

import com.otzar.sscm.entities.InstagramConnectionSettings;
import com.otzar.sscm.models.InstagramSettingsRequest;
import com.otzar.sscm.models.InstagramSettingsResponse;
import com.otzar.sscm.repository.InstagramConnectionSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class InstagramConnectionSettingsService {
    private static final String DEFAULT_GRAPH_API_BASE_URL = "https://graph.facebook.com/v25.0";
    private final InstagramConnectionSettingsRepository repository;
    private final String defaultInstagramUserId;
    private final String environmentGraphApiBaseUrl;
    private final String accessToken;

    public InstagramConnectionSettingsService(
            InstagramConnectionSettingsRepository repository,
            @Value("${META_INSTAGRAM_USER_ID:}") String instagramUserId,
            @Value("${META_GRAPH_API_BASE_URL:${META_GRAPH_API_BASE:}}") String graphApiBaseUrl,
            @Value("${META_PAGE_ACCESS_TOKEN:}") String accessToken) {
        this.repository = repository;
        this.defaultInstagramUserId = clean(instagramUserId);
        this.environmentGraphApiBaseUrl = trimSlash(graphApiBaseUrl);
        this.accessToken = clean(accessToken);
    }

    @Transactional(readOnly = true)
    public InstagramSettingsResponse get() {
        return effective(repository.find().orElse(null));
    }

    @Transactional
    public InstagramSettingsResponse update(InstagramSettingsRequest request) {
        InstagramConnectionSettings settings = repository.find().orElseGet(InstagramConnectionSettings::new);
        settings.setInstagramUserId(clean(request.getInstagramUserId()));
        settings.setGraphApiBaseUrl(trimSlash(request.getGraphApiBaseUrl()));
        settings.setUpdatedAt(LocalDateTime.now());
        repository.save(settings);
        return effective(settings);
    }

    public String instagramUserId() { return get().instagramUserId; }
    public String graphApiBaseUrl() { return get().graphApiBaseUrl; }
    public String accessToken() { return accessToken; }
    public String instagramUserIdSource() { return get().instagramUserIdSource; }
    public String graphApiBaseUrlSource() { return get().graphApiBaseUrlSource; }

    private InstagramSettingsResponse effective(InstagramConnectionSettings settings) {
        String databaseId = settings == null ? "" : clean(settings.getInstagramUserId());
        String databaseUrl = settings == null ? "" : trimSlash(settings.getGraphApiBaseUrl());
        boolean validDatabaseId = validInstagramUserId(databaseId);
        boolean validEnvironmentId = validInstagramUserId(defaultInstagramUserId);
        boolean validDatabaseUrl = validGraphApiBaseUrl(databaseUrl);
        boolean validEnvironmentUrl = validGraphApiBaseUrl(environmentGraphApiBaseUrl);

        String userId = validDatabaseId ? databaseId : validEnvironmentId ? defaultInstagramUserId : "";
        String userIdSource = validDatabaseId ? "DATABASE" : validEnvironmentId ? "ENV" : "MISSING";
        String baseUrl = validDatabaseUrl ? databaseUrl
                : validEnvironmentUrl ? environmentGraphApiBaseUrl : DEFAULT_GRAPH_API_BASE_URL;
        String baseUrlSource = validDatabaseUrl ? "DATABASE" : validEnvironmentUrl ? "ENV" : "DEFAULT";
        LocalDateTime updatedAt = settings == null ? null : settings.getUpdatedAt();
        return new InstagramSettingsResponse(userId, baseUrl, !accessToken.isEmpty(), updatedAt,
                userIdSource, baseUrlSource);
    }
    private static boolean validInstagramUserId(String value) { return value.matches("^[0-9]{5,40}$"); }
    private static boolean validGraphApiBaseUrl(String value) {
        return value.matches("^https://graph\\.facebook\\.com/v[0-9]+\\.[0-9]+$");
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String trimSlash(String value) { return clean(value).replaceAll("/+$", ""); }
}
