package com.otzar.sscm.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationDiagnosticsTests {
    @Test
    void startupDiagnosticsReportPresenceWithoutLoggingSecrets() throws Exception {
        String token = "token-that-must-not-be-logged";
        String secret = "secret-that-must-not-be-logged";
        ConfigurationDiagnostics diagnostics = new ConfigurationDiagnostics(
                token, "ig-user", "https://graph.facebook.com/v25.0", "cloud", "key", secret);

        Logger logger = (Logger) LoggerFactory.getLogger(ConfigurationDiagnostics.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            diagnostics.run(new DefaultApplicationArguments(new String[0]));
        } finally {
            logger.detachAppender(appender);
        }

        String messages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
        assertTrue(messages.contains("Meta access token present: yes"));
        assertTrue(messages.contains("Meta environment configuration complete: yes"));
        assertTrue(messages.contains("Cloudinary configuration present: yes"));
        assertTrue(messages.contains("Environment Instagram user ID present: yes"));
        assertTrue(messages.contains("Versioned Meta Graph API URL valid: yes"));
        assertTrue(messages.contains("Instagram account ID source: ENV"));
        assertTrue(messages.contains("Instagram account ID present: true"));
        assertTrue(messages.contains("Graph API base source: ENV"));
        assertTrue(messages.contains("Meta token present: true"));
        assertFalse(messages.contains(token));
        assertFalse(messages.contains(secret));
        assertFalse(messages.toLowerCase().contains("authorization"));
    }
}
