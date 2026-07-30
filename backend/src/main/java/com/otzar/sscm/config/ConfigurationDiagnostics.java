package com.otzar.sscm.config;

import com.otzar.sscm.models.InstagramSettingsResponse;
import com.otzar.sscm.service.InstagramConnectionSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationDiagnostics implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationDiagnostics.class);

    private final String metaToken;
    private final String instagramUserId;
    private final String metaBaseUrl;
    private final String cloudName;
    private final String cloudKey;
    private final String cloudSecret;
    private final InstagramConnectionSettingsService connectionSettings;

    @Autowired
    public ConfigurationDiagnostics(
            @Value("${META_PAGE_ACCESS_TOKEN:}") String metaToken,
            @Value("${META_INSTAGRAM_USER_ID:}") String instagramUserId,
            @Value("${META_GRAPH_API_BASE_URL:${META_GRAPH_API_BASE:https://graph.facebook.com/v25.0}}") String metaBaseUrl,
            @Value("${CLOUDINARY_CLOUD_NAME:}") String cloudName,
            @Value("${CLOUDINARY_API_KEY:}") String cloudKey,
            @Value("${CLOUDINARY_API_SECRET:}") String cloudSecret,
            InstagramConnectionSettingsService connectionSettings) {
        this.metaToken = clean(metaToken);
        this.instagramUserId = clean(instagramUserId);
        this.metaBaseUrl = clean(metaBaseUrl);
        this.cloudName = clean(cloudName);
        this.cloudKey = clean(cloudKey);
        this.cloudSecret = clean(cloudSecret);
        this.connectionSettings = connectionSettings;
    }

    public ConfigurationDiagnostics(String metaToken, String instagramUserId, String metaBaseUrl,
                                    String cloudName, String cloudKey, String cloudSecret) {
        this.metaToken = clean(metaToken);
        this.instagramUserId = clean(instagramUserId);
        this.metaBaseUrl = clean(metaBaseUrl);
        this.cloudName = clean(cloudName);
        this.cloudKey = clean(cloudKey);
        this.cloudSecret = clean(cloudSecret);
        this.connectionSettings = null;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean graphUrlValid = metaBaseUrl.matches(
                "^https://graph\\.facebook\\.com/v[0-9]+\\.[0-9]+/?$");
        logger.info("Meta access token present: {}", yesNo(!metaToken.isEmpty()));
        logger.info("Environment Instagram user ID present: {}", yesNo(!instagramUserId.isEmpty()));
        logger.info("Versioned Meta Graph API URL valid: {}", yesNo(graphUrlValid));
        logger.info("Meta environment configuration complete: {}", yesNo(
                !metaToken.isEmpty() && !instagramUserId.isEmpty() && graphUrlValid));
        logEffectiveInstagramConfiguration(graphUrlValid);
        logger.info("Cloudinary configuration present: {}", yesNo(
                !cloudName.isEmpty() && !cloudKey.isEmpty() && !cloudSecret.isEmpty()));
    }

    private void logEffectiveInstagramConfiguration(boolean graphUrlValid) {
        if (connectionSettings == null) {
            logger.info("Instagram account ID source: {}",
                    instagramUserId.isEmpty() ? "MISSING" : "ENV");
            logger.info("Instagram account ID present: {}", !instagramUserId.isEmpty());
            logger.info("Graph API base source: {}", graphUrlValid ? "ENV" : "DEFAULT");
            logger.info("Meta token present: {}", !metaToken.isEmpty());
            return;
        }
        try {
            InstagramSettingsResponse settings = connectionSettings.get();
            logger.info("Instagram account ID source: {}", settings.instagramUserIdSource);
            logger.info("Instagram account ID present: {}", !settings.instagramUserId.isEmpty());
            logger.info("Graph API base source: {}", settings.graphApiBaseUrlSource);
            logger.info("Meta token present: {}", settings.accessTokenConfigured);
        } catch (RuntimeException exception) {
            logger.warn("Instagram configuration source unavailable: {}",
                    exception.getClass().getSimpleName());
            logger.info("Instagram account ID source: {}",
                    instagramUserId.isEmpty() ? "MISSING" : "ENV");
            logger.info("Instagram account ID present: {}", !instagramUserId.isEmpty());
            logger.info("Graph API base source: {}", graphUrlValid ? "ENV" : "DEFAULT");
            logger.info("Meta token present: {}", !metaToken.isEmpty());
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String yesNo(boolean present) {
        return present ? "yes" : "no";
    }
}
