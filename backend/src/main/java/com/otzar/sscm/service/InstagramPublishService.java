package com.otzar.sscm.service;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.models.InstagramPublishResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@Service
public class InstagramPublishService {
    private final ContentService contentService;
    private final RestTemplate restTemplate;
    private final String instagramUserId;
    private final String accessToken;
    private final String graphApiBaseUrl;
    private final InstagramConnectionSettingsService connectionSettings;

    @Autowired
    public InstagramPublishService(
            ContentService contentService,
            @Value("${META_INSTAGRAM_USER_ID:}") String instagramUserId,
            @Value("${META_PAGE_ACCESS_TOKEN:}") String accessToken,
            @Value("${META_GRAPH_API_BASE_URL:${META_GRAPH_API_BASE:https://graph.facebook.com/v25.0}}") String graphApiBaseUrl,
            InstagramConnectionSettingsService connectionSettings) {
        this(contentService, new RestTemplate(), instagramUserId, accessToken, graphApiBaseUrl,
                connectionSettings);
    }

    public InstagramPublishService(ContentService contentService, RestTemplate restTemplate,
                                   String instagramUserId, String accessToken, String graphApiBaseUrl) {
        this(contentService, restTemplate, instagramUserId, accessToken, graphApiBaseUrl, null);
    }

    private InstagramPublishService(ContentService contentService, RestTemplate restTemplate,
                                   String instagramUserId, String accessToken, String graphApiBaseUrl,
                                   InstagramConnectionSettingsService connectionSettings) {
        this.contentService = contentService;
        this.restTemplate = restTemplate;
        this.instagramUserId = trim(instagramUserId);
        this.accessToken = trim(accessToken);
        this.graphApiBaseUrl = trimTrailingSlash(graphApiBaseUrl);
        this.connectionSettings = connectionSettings;
    }

    public InstagramPublishResponse publish(Long contentId) {
        requireConfiguration();
        Content content = contentService.findById(contentId)
                .orElseThrow(() -> new InstagramPublishException(
                        InstagramPublishException.Reason.CONTENT_NOT_FOUND, "Content not found"));
        validateContent(content);

        boolean video = isVideo(content);
        MultiValueMap<String, String> container = form(
                video ? "video_url" : "image_url", content.getFile_url(),
                "caption", content.getDescription() == null ? "" : content.getDescription());
        if (video) container.add("media_type", "REELS");
        String creationId = postForId("media", container);
        if (video) waitUntilReady(creationId);
        String mediaId = postForId("media_publish", form(
                "creation_id", creationId));
        return new InstagramPublishResponse(true, mediaId);
    }

    private void waitUntilReady(String creationId) {
        for (int attempt = 0; attempt < 30; attempt++) {
            URI uri = UriComponentsBuilder.fromHttpUrl(currentGraphApiBaseUrl())
                    .pathSegment(creationId)
                    .queryParam("fields", "status_code,status")
                    .build().encode().toUri();
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(currentAccessToken());
                ResponseEntity<Map> response = restTemplate.exchange(
                        uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
                Map body = response.getBody();
                String status = body == null ? "" : String.valueOf(body.get("status_code"));
                if ("FINISHED".equals(status)) return;
                if ("ERROR".equals(status) || "EXPIRED".equals(status)) {
                    throw new InstagramPublishException(
                            InstagramPublishException.Reason.MEDIA_PROCESSING_FAILED,
                            "Instagram could not process the video");
                }
                Thread.sleep(2000);
            } catch (InstagramPublishException exception) {
                throw exception;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new InstagramPublishException(
                        InstagramPublishException.Reason.MEDIA_PROCESSING_FAILED,
                        "Video processing was interrupted", exception);
            } catch (RestClientException exception) {
                throw new InstagramPublishException(
                        InstagramPublishException.Reason.META_API_FAILURE,
                        "Could not check Instagram video processing", exception);
            }
        }
        throw new InstagramPublishException(
                InstagramPublishException.Reason.MEDIA_PROCESSING_FAILED,
                "Instagram video processing timed out");
    }

    private String postForId(String action, MultiValueMap<String, String> form) {
        URI uri = UriComponentsBuilder.fromHttpUrl(currentGraphApiBaseUrl())
                .pathSegment(currentInstagramUserId(), action)
                .build()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(currentAccessToken());
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    uri, new HttpEntity<>(form, headers), Map.class);
            Object id = response.getBody() == null ? null : response.getBody().get("id");
            if (id == null || id.toString().trim().isEmpty()) {
                throw new InstagramPublishException(
                        InstagramPublishException.Reason.META_API_FAILURE,
                        "Meta API returned an invalid response");
            }
            return id.toString();
        } catch (InstagramPublishException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new InstagramPublishException(
                    InstagramPublishException.Reason.META_API_FAILURE,
                    "Instagram publishing failed at Meta", exception);
        }
    }

    private void validateContent(Content content) {
        if (content.getStatus() != ContentStatus.APPROVED) {
            throw new InstagramPublishException(
                    InstagramPublishException.Reason.CONTENT_NOT_APPROVED,
                    "Only approved content can be published to Instagram");
        }
        if (content.getFile_url() == null || content.getFile_url().trim().isEmpty()) {
            throw new InstagramPublishException(
                    InstagramPublishException.Reason.IMAGE_REQUIRED,
                    "Media is required for Instagram publishing");
        }
        String type = content.getContent_type() == null ? "" : content.getContent_type().toUpperCase();
        if (!type.equals("IMAGE") && !type.equals("VIDEO") && !type.equals("REEL")) {
            throw new InstagramPublishException(
                    InstagramPublishException.Reason.UNSUPPORTED_MEDIA,
                    "Only images, videos, and reels can be published");
        }
        if (!isPublicHttpsUrl(content.getFile_url())) {
            throw new InstagramPublishException(
                    InstagramPublishException.Reason.IMAGE_NOT_PUBLIC,
                    "The image must have a public HTTPS URL");
        }
    }

    private boolean isVideo(Content content) {
        String type = content.getContent_type() == null ? "" : content.getContent_type();
        return "VIDEO".equalsIgnoreCase(type) || "REEL".equalsIgnoreCase(type);
    }

    private boolean isPublicHttpsUrl(String value) {
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null) return false;
            String normalized = host.toLowerCase();
            return !normalized.equals("localhost")
                    && !normalized.equals("127.0.0.1")
                    && !normalized.startsWith("10.")
                    && !normalized.startsWith("192.168.")
                    && !normalized.matches("^172\\.(1[6-9]|2\\d|3[01])\\..*");
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private void requireConfiguration() {
        if (currentInstagramUserId().isEmpty() || currentAccessToken().isEmpty()
                || currentGraphApiBaseUrl().isEmpty()) {
            throw new InstagramPublishException(
                    InstagramPublishException.Reason.NOT_CONFIGURED,
                    "Instagram publishing is not configured");
        }
    }

    private MultiValueMap<String, String> form(String... values) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (int index = 0; index < values.length; index += 2) {
            form.add(values[index], values[index + 1]);
        }
        return form;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimTrailingSlash(String value) {
        String result = trim(value);
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }
    private String currentInstagramUserId() {
        return connectionSettings == null ? instagramUserId : trim(connectionSettings.instagramUserId());
    }
    private String currentGraphApiBaseUrl() {
        return connectionSettings == null ? graphApiBaseUrl
                : trimTrailingSlash(connectionSettings.graphApiBaseUrl());
    }
    private String currentAccessToken() {
        return connectionSettings == null ? accessToken : trim(connectionSettings.accessToken());
    }
}
