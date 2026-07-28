package com.otzar.sscm.config;

import com.otzar.sscm.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer webMvcConfigurer(
            FileStorageService fileStorageService,
            @Value("${sscm.cors.allowed-origins:}") String allowedOriginsValue,
            @Value("${sscm.cors.allowed-origin-patterns:}") String allowedPatternsValue) {
        String[] allowedOrigins = split(allowedOriginsValue);
        String[] allowedPatterns = split(allowedPatternsValue);
        rejectCredentialedWildcard(allowedOrigins);
        rejectCredentialedWildcard(allowedPatterns);

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedOriginPatterns(allowedPatterns)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("Content-Type", "Authorization", "Accept", "X-Requested-With")
                        .allowCredentials(true)
                        .maxAge(3600);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations(fileStorageService.getUploadDirectory().toUri().toString());
            }
        };
    }

    static String[] split(String value) {
        if (value == null || value.trim().isEmpty()) return new String[0];
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .toArray(String[]::new);
    }

    private static void rejectCredentialedWildcard(String[] values) {
        if (Arrays.asList(values).contains("*")) {
            throw new IllegalStateException(
                    "Credentialed CORS configuration must not use an unrestricted wildcard");
        }
    }
}
