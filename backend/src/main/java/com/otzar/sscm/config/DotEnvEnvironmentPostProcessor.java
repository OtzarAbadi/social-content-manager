package com.otzar.sscm.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads backend/.env below system properties and operating-system environment variables.
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    static final String PROPERTY_SOURCE_NAME = "backendDotEnv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (String profile : environment.getActiveProfiles()) {
            if ("test".equalsIgnoreCase(profile)) return;
        }
        Path dotenv = findDotEnv(Paths.get(System.getProperty("user.dir", ".")));
        Map<String, Object> values = load(dotenv);
        if (!values.isEmpty()) {
            environment.getPropertySources().addAfter(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new MapPropertySource(PROPERTY_SOURCE_NAME, values));
        }
    }

    static Path findDotEnv(Path workingDirectory) {
        Path direct = workingDirectory.resolve(".env");
        if (workingDirectory.getFileName() != null
                && "backend".equalsIgnoreCase(workingDirectory.getFileName().toString())) {
            return direct;
        }
        return workingDirectory.resolve("backend").resolve(".env");
    }

    static Map<String, Object> load(Path path) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (!Files.isRegularFile(path)) return values;
        try {
            for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("export ")) line = line.substring(7).trim();
                int separator = line.indexOf('=');
                if (separator <= 0) continue;
                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isEmpty()) values.put(key, value);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read backend/.env", exception);
        }
        return values;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
