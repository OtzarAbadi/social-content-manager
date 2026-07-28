package com.otzar.sscm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public ConfigurationDiagnostics(
            @Value("${META_PAGE_ACCESS_TOKEN:}") String metaToken,
            @Value("${META_INSTAGRAM_USER_ID:}") String instagramUserId,
            @Value("${META_GRAPH_API_BASE_URL:https://graph.facebook.com/v25.0}") String metaBaseUrl,
            @Value("${CLOUDINARY_CLOUD_NAME:}") String cloudName,
            @Value("${CLOUDINARY_API_KEY:}") String cloudKey,
            @Value("${CLOUDINARY_API_SECRET:}") String cloudSecret) {
        this.metaToken = clean(metaToken);
        this.instagramUserId = clean(instagramUserId);
        this.metaBaseUrl = clean(metaBaseUrl);
        this.cloudName = clean(cloudName);
        this.cloudKey = clean(cloudKey);
        this.cloudSecret = clean(cloudSecret);
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info("Meta configuration present: {}", yesNo(
                !metaToken.isEmpty() && !instagramUserId.isEmpty() && !metaBaseUrl.isEmpty()));
        logger.info("Cloudinary configuration present: {}", yesNo(
                !cloudName.isEmpty() && !cloudKey.isEmpty() && !cloudSecret.isEmpty()));
        logger.info("Instagram user ID present: {}", yesNo(!instagramUserId.isEmpty()));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String yesNo(boolean present) {
        return present ? "yes" : "no";
    }
}
