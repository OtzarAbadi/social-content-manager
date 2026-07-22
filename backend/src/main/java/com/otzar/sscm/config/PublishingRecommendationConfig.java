package com.otzar.sscm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PublishingRecommendationConfig {
    @Bean
    public Clock publishingRecommendationClock() {
        return Clock.systemUTC();
    }
}
