package com.otzar.sscm.config;

import com.otzar.sscm.models.PublishingProviderType;
import com.otzar.sscm.service.PublishingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Locale;

@Configuration
public class SocialPublishingConfig {
    @Bean
    @Primary
    public PublishingProvider activePublishingProvider(
            List<PublishingProvider> providers,
            @Value("${sscm.publishing.provider:LOCAL}") String configuredProvider) {
        PublishingProviderType requestedType;
        try {
            requestedType = PublishingProviderType.valueOf(
                    configuredProvider.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Unsupported publishing provider: " + configuredProvider, ex);
        }

        return providers.stream()
                .filter(provider -> provider.getType() == requestedType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No publishing provider registered for " + requestedType));
    }
}
