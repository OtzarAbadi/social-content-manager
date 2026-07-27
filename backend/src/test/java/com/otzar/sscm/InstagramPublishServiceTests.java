package com.otzar.sscm;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.models.InstagramPublishResponse;
import com.otzar.sscm.service.ContentService;
import com.otzar.sscm.service.InstagramPublishException;
import com.otzar.sscm.service.InstagramPublishService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InstagramPublishServiceTests {
    @Test
    void createsContainerThenPublishesUsingMockedMetaResponses() {
        ContentService contentService = mock(ContentService.class);
        when(contentService.findById(12L)).thenReturn(Optional.of(approvedImage()));
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(once(), requestTo("https://graph.example/v25.0/ig-user/media"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"creation-123\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://graph.example/v25.0/ig-user/media_publish"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"media-456\"}", MediaType.APPLICATION_JSON));
        InstagramPublishService service = new InstagramPublishService(
                contentService, restTemplate, "ig-user", "secret-token",
                "https://graph.example/v25.0");

        InstagramPublishResponse result = service.publish(12L);

        assertEquals(true, result.isSuccess());
        assertEquals("media-456", result.getInstagramMediaId());
        server.verify();
    }

    @Test
    void metaFailureIsMappedToSafeException() {
        ContentService contentService = mock(ContentService.class);
        when(contentService.findById(12L)).thenReturn(Optional.of(approvedImage()));
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://graph.example/v25.0/ig-user/media"))
                .andRespond(withServerError());
        InstagramPublishService service = new InstagramPublishService(
                contentService, restTemplate, "ig-user", "secret-token",
                "https://graph.example/v25.0");

        InstagramPublishException exception = assertThrows(
                InstagramPublishException.class, () -> service.publish(12L));

        assertEquals(InstagramPublishException.Reason.META_API_FAILURE, exception.getReason());
        assertEquals("Instagram publishing failed at Meta", exception.getMessage());
        server.verify();
    }

    @Test
    void localUploadUrlIsRejectedBeforeMetaCall() {
        ContentService contentService = mock(ContentService.class);
        Content content = approvedImage();
        content.setFile_url("/uploads/local.jpg");
        when(contentService.findById(12L)).thenReturn(Optional.of(content));
        InstagramPublishService service = new InstagramPublishService(
                contentService, new RestTemplate(), "ig-user", "secret-token",
                "https://graph.example/v25.0");

        InstagramPublishException exception = assertThrows(
                InstagramPublishException.class, () -> service.publish(12L));

        assertEquals(InstagramPublishException.Reason.IMAGE_NOT_PUBLIC, exception.getReason());
    }

    private Content approvedImage() {
        Content content = new Content();
        content.setContent_id(12L);
        content.setStatus(ContentStatus.APPROVED);
        content.setContent_type("IMAGE");
        content.setDescription("Caption");
        content.setFile_url("https://res.cloudinary.com/demo/image/upload/example.jpg");
        return content;
    }
}
