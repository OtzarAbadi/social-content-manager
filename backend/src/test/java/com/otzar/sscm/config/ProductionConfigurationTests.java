package com.otzar.sscm.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ProductionConfigurationTests {
    @Test
    void localIsTheDefaultProfileAndProductionMustBeExplicit() throws Exception {
        Properties properties = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application.properties"));
        assertEquals("local", properties.getProperty("spring.profiles.default"));
        assertEquals(null, properties.getProperty("spring.profiles.active"));
    }

    @Test
    void hibernateConfigurationIsAvailableForEveryNonTestProfile() {
        Profile profile = AppConfig.class.getAnnotation(Profile.class);
        assertArrayEquals(new String[]{"!test"}, profile.value());
    }

    @Test
    void railwayPortTakesPrecedenceOverLocalServerPort() throws Exception {
        Properties properties = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application-production.properties"));
        assertEquals("${PORT:${SERVER_PORT:8081}}", properties.getProperty("server.port"));
        assertEquals("0.0.0.0", properties.getProperty("server.address"));
        assertEquals("/api", properties.getProperty("server.servlet.context-path"));
        assertEquals("${SPRING_DATASOURCE_URL}", properties.getProperty("spring.datasource.url"));
        assertEquals("${SPRING_DATASOURCE_USERNAME}", properties.getProperty("spring.datasource.username"));
        assertEquals("${SPRING_DATASOURCE_PASSWORD}", properties.getProperty("spring.datasource.password"));
        assertEquals("none", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("never", properties.getProperty("spring.sql.init.mode"));
    }

    @Test
    void localProfileProvidesCompleteDatasourceConfiguration() throws Exception {
        Properties properties = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("application-local.properties"));
        assertEquals("com.mysql.cj.jdbc.Driver",
                properties.getProperty("spring.datasource.driver-class-name"));
        assertEquals("${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/social_content_manager?useSSL=false&allowPublicKeyRetrieval=true}",
                properties.getProperty("spring.datasource.url"));
        assertEquals("${SPRING_DATASOURCE_USERNAME:root}",
                properties.getProperty("spring.datasource.username"));
        assertEquals("${SPRING_DATASOURCE_PASSWORD}",
                properties.getProperty("spring.datasource.password"));
    }

    @Test
    void acceptsRailwayStyleMysqlSchemeAndKeepsJdbcUrlsStable() {
        assertEquals(
                "jdbc:mysql://mysql.railway.internal:3306/railway",
                AppConfig.normalizeJdbcUrl("mysql://mysql.railway.internal:3306/railway"));
        assertEquals(
                "jdbc:mysql://localhost:3306/social_content_manager",
                AppConfig.normalizeJdbcUrl("jdbc:mysql://localhost:3306/social_content_manager"));
    }
}
