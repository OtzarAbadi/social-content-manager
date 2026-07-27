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

    @Autowired
    public InstagramPublishService(
            ContentService contentService,
            @Value("${META_INSTAGRAM_USER_ID:}") String instagramUserId,
            @Value("${META_PAGE_ACCESS_TOKEN:}") String accessToken,
            @Value("${META_GRAPH_API_BASE_URL:https://graph.facebook.com/v25.0}") String graphApiBaseUrl) {
        this(contentService, new RestTemplate(), instagramUserId, accessToken, graphApiBaseUrl);
    }

    public InstagramPublishService(ContentService contentService, RestTemplate restTemplate,
                                   String instagramUserId, String accessToken, String graphApiBaseUrl) {
        this.contentService = contentService;
        this.restTemplate = restTemplate;
        this.instagramUserId = trim(instagramUserId);
        this.accessToken = trim(accessToken);
        this.graphApiBaseUrl = trimTrailingSlash(graphApiBaseUrl);
    }

    public InstagramPublishResponse publish(Long contentId) {
        requireConfiguration();
        Content content = contentService.findById(contentId)
                .orElseThrow(() -> new InstagramPublishException(
                        InstagramPublishException.Reason.CONTENT_NOT_FOUND, "Content not found"));
        validateContent(content);

        String creationId = postForId("media", form(
                "image_url", content.getFile_url(),
                "caption", content.getDescription() == null ? "" : content.getDescription(),
                "access_token", accessToken));
        String mediaId = postForId("media_publish", form(
                "creation_id", creationId,
                "access_token", accessToken));
        return new InstagramPublishResponse(true, mediaId);
    }

    private String postForId(String action, MultiValueMap<String, String> form) {
        URI uri = UriComponentsBuilder.fromHttpUrl(graphApiBaseUrl)
                .pathSegment(instagramUserId, action)
                .build()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
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
        if (!"IMAGE".equalsIgnoreCase(content.getContent_type())
                || content.getFile_url() == null || content.getFile_url().trim().isEmpty()) {
            throw new InstagramPublishException(
                    InstagramPublishException.Reason.IMAGE_REQUIRED,
                    "An image is required for Instagram publishing");
        }
        if (!isPublicHttpsUrl(content.getFile_url())) {
            throw new InstagramPublishException(
                    InstagramPublishException.Reason.IMAGE_NOT_PUBLIC,
                    "The image must have a public HTTPS URL");
        }
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
        if (instagramUserId.isEmpty() || accessToken.isEmpty() || graphApiBaseUrl.isEmpty()) {
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
}
