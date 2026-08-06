package com.otzar.sscm;

import com.otzar.sscm.entities.InstagramConnectionSettings;
import com.otzar.sscm.models.InstagramSettingsResponse;
import com.otzar.sscm.repository.InstagramConnectionSettingsRepository;
import com.otzar.sscm.service.InstagramConnectionSettingsService;
import com.otzar.sscm.service.ClientService;
import com.otzar.sscm.service.InstagramInsightsException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InstagramConnectionSettingsServiceTests {
    @Test
    void otzarClientUsesEnvironmentConfigurationWhenPersistedSettingsAreMissing() {
        InstagramConnectionSettingsRepository repository = mock(InstagramConnectionSettingsRepository.class);
        ClientService clients = mock(ClientService.class);
        when(clients.isLinkedToUsername(7L, "otzar")).thenReturn(true);
        when(repository.findByClientId(7L)).thenReturn(Optional.empty());
        InstagramSettingsResponse response = new InstagramConnectionSettingsService(repository,
                "178900000000002", "https://graph.facebook.com/v25.0", "unchanged-token", clients).get(7L);
        assertEquals("178900000000002", response.instagramUserId);
        assertEquals("ENV", response.instagramUserIdSource);
        assertEquals("https://graph.facebook.com/v25.0", response.graphApiBaseUrl);
        assertEquals(true, response.accessTokenConfigured);
    }

    @Test
    void nonOtzarClientNeverReceivesEnvironmentFallback() {
        InstagramConnectionSettingsRepository repository = mock(InstagramConnectionSettingsRepository.class);
        ClientService clients = mock(ClientService.class);
        when(clients.isLinkedToUsername(8L, "otzar")).thenReturn(false);
        InstagramInsightsException error = assertThrows(InstagramInsightsException.class, () ->
                new InstagramConnectionSettingsService(repository, "178900000000002",
                        "https://graph.facebook.com/v25.0", "unchanged-token", clients).get(8L));
        assertEquals("INSTAGRAM_NOT_CONNECTED", error.getCode());
        verify(repository, never()).findByClientId(8L);
    }
    @Test
    void validDatabaseValuesTakePriority() {
        InstagramConnectionSettings settings = settings(
                "178900000000001", "https://graph.facebook.com/v24.0");
        InstagramSettingsResponse response = service(Optional.of(settings)).get();

        assertEquals("178900000000001", response.instagramUserId);
        assertEquals("DATABASE", response.instagramUserIdSource);
        assertEquals("https://graph.facebook.com/v24.0", response.graphApiBaseUrl);
        assertEquals("DATABASE", response.graphApiBaseUrlSource);
    }

    @Test
    void invalidOrEmptyDatabaseValuesCannotOverrideEnvironment() {
        InstagramConnectionSettings settings = settings("", "https://example.invalid/v1.0");
        InstagramSettingsResponse response = service(Optional.of(settings)).get();

        assertEquals("178900000000002", response.instagramUserId);
        assertEquals("ENV", response.instagramUserIdSource);
        assertEquals("https://graph.facebook.com/v25.0", response.graphApiBaseUrl);
        assertEquals("ENV", response.graphApiBaseUrlSource);
    }

    @Test
    void missingGraphEnvironmentUsesSafeDefault() {
        InstagramConnectionSettingsRepository repository = mock(InstagramConnectionSettingsRepository.class);
        when(repository.find()).thenReturn(Optional.empty());
        InstagramSettingsResponse response = new InstagramConnectionSettingsService(
                repository, "", "", "configured-token").get();

        assertEquals("MISSING", response.instagramUserIdSource);
        assertEquals("https://graph.facebook.com/v25.0", response.graphApiBaseUrl);
        assertEquals("DEFAULT", response.graphApiBaseUrlSource);
    }

    private InstagramConnectionSettingsService service(Optional<InstagramConnectionSettings> settings) {
        InstagramConnectionSettingsRepository repository = mock(InstagramConnectionSettingsRepository.class);
        when(repository.find()).thenReturn(settings);
        return new InstagramConnectionSettingsService(repository, "178900000000002",
                "https://graph.facebook.com/v25.0", "configured-token");
    }

    private InstagramConnectionSettings settings(String accountId, String graphUrl) {
        InstagramConnectionSettings settings = new InstagramConnectionSettings();
        settings.setInstagramUserId(accountId);
        settings.setGraphApiBaseUrl(graphUrl);
        return settings;
    }
}
