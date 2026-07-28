package com.otzar.sscm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DotEnvEnvironmentPostProcessorTests {
    @TempDir
    Path tempDirectory;

    @Test
    void operatingSystemEnvironmentOverridesDotEnv() throws Exception {
        Path dotenv = tempDirectory.resolve("backend").resolve(".env");
        Files.createDirectories(dotenv.getParent());
        Files.writeString(dotenv, "META_PAGE_ACCESS_TOKEN=dotenv-token\n");

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().replace(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new MapPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        Map.of("META_PAGE_ACCESS_TOKEN", "environment-token")));
        environment.getPropertySources().addAfter(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new MapPropertySource(DotEnvEnvironmentPostProcessor.PROPERTY_SOURCE_NAME,
                        DotEnvEnvironmentPostProcessor.load(dotenv)));

        assertEquals("environment-token", environment.getProperty("META_PAGE_ACCESS_TOKEN"));
    }

    @Test
    void missingDotEnvIsIgnored() {
        assertTrue(DotEnvEnvironmentPostProcessor.load(
                tempDirectory.resolve("backend").resolve(".env")).isEmpty());
    }

    @Test
    void testProfileDoesNotLoadLocalDotEnv() throws Exception {
        Path dotenv = tempDirectory.resolve("backend").resolve(".env");
        Files.createDirectories(dotenv.getParent());
        Files.writeString(dotenv, "CLOUDINARY_API_SECRET=local-only-secret\n");

        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("test");
        String originalUserDirectory = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDirectory.toString());
            new DotEnvEnvironmentPostProcessor().postProcessEnvironment(
                    environment, new SpringApplication());
        } finally {
            System.setProperty("user.dir", originalUserDirectory);
        }

        assertTrue(environment.getProperty("CLOUDINARY_API_SECRET", "").isEmpty());
    }
}
