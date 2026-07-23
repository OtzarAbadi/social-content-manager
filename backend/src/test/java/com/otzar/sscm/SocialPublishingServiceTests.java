package com.otzar.sscm;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.models.PublishingProviderType;
import com.otzar.sscm.models.PublishingRequest;
import com.otzar.sscm.models.PublishingResult;
import com.otzar.sscm.service.ContentService;
import com.otzar.sscm.service.LocalPublishingProvider;
import com.otzar.sscm.service.PublishingProvider;
import com.otzar.sscm.service.SocialPublishingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocialPublishingServiceTests {
    @Test
    void localProviderReturnsSuccessfulLocalResultWithoutExternalPostId() {
        PublishingRequest request = new PublishingRequest(
                "content:9", 9L, 3L, "Title", "Description", "TEXT", null, null);

        PublishingResult result = new LocalPublishingProvider().publish(request);

        assertTrue(result.isSuccess());
        assertEquals(PublishingProviderType.LOCAL, result.getProviderType());
        assertEquals(null, result.getExternalPostId());
    }

    @Test
    void approvedContentIsPassedToProviderBeforeExistingPublishTransition() {
        ContentService contentService = mock(ContentService.class);
        PublishingProvider provider = mock(PublishingProvider.class);
        SocialPublishingService service = new SocialPublishingService(contentService, provider);
        Content content = approvedContent();
        when(contentService.findById(12L)).thenReturn(Optional.of(content));
        when(provider.publish(org.mockito.ArgumentMatchers.any(PublishingRequest.class)))
                .thenReturn(PublishingResult.success(PublishingProviderType.LOCAL, null, LocalDateTime.now()));
        when(contentService.publish(12L, 7L)).thenReturn(Optional.of(content));

        Optional<Content> result = service.publish(12L, 7L);

        assertTrue(result.isPresent());
        ArgumentCaptor<PublishingRequest> request = ArgumentCaptor.forClass(PublishingRequest.class);
        verify(provider).publish(request.capture());
        assertEquals("content:12", request.getValue().getRequestId());
        assertEquals(4L, request.getValue().getClientId());
        assertEquals("Provider-ready content", request.getValue().getTitle());
        verify(contentService).publish(12L, 7L);
    }

    @Test
    void providerFailureDoesNotRunStatusTransition() {
        ContentService contentService = mock(ContentService.class);
        PublishingProvider provider = mock(PublishingProvider.class);
        SocialPublishingService service = new SocialPublishingService(contentService, provider);
        when(contentService.findById(12L)).thenReturn(Optional.of(approvedContent()));
        when(provider.publish(org.mockito.ArgumentMatchers.any(PublishingRequest.class)))
                .thenReturn(PublishingResult.failure(PublishingProviderType.LOCAL, "LOCAL_FAILURE", "Failed"));

        assertThrows(IllegalStateException.class, () -> service.publish(12L, 7L));
        verify(contentService, never()).publish(12L, 7L);
    }

    @Test
    void unapprovedContentNeverReachesProvider() {
        ContentService contentService = mock(ContentService.class);
        PublishingProvider provider = mock(PublishingProvider.class);
        SocialPublishingService service = new SocialPublishingService(contentService, provider);
        Content content = approvedContent();
        content.setStatus(ContentStatus.DRAFT);
        when(contentService.findById(12L)).thenReturn(Optional.of(content));

        assertThrows(IllegalStateException.class, () -> service.publish(12L, 7L));
        verify(provider, never()).publish(org.mockito.ArgumentMatchers.any(PublishingRequest.class));
        verify(contentService, never()).publish(12L, 7L);
    }

    @Test
    void missingContentReturnsEmptyWithoutProviderCall() {
        ContentService contentService = mock(ContentService.class);
        PublishingProvider provider = mock(PublishingProvider.class);
        SocialPublishingService service = new SocialPublishingService(contentService, provider);
        when(contentService.findById(99L)).thenReturn(Optional.empty());

        assertFalse(service.publish(99L, 7L).isPresent());
        verify(provider, never()).publish(org.mockito.ArgumentMatchers.any(PublishingRequest.class));
    }

    private Content approvedContent() {
        Content content = new Content();
        content.setContent_id(12L);
        content.setClientId(4L);
        content.setTitle("Provider-ready content");
        content.setDescription("Description");
        content.setContent_type("TEXT");
        content.setStatus(ContentStatus.APPROVED);
        return content;
    }
}
