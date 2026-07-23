package com.otzar.sscm.service;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.models.PublishingRequest;
import com.otzar.sscm.models.PublishingResult;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SocialPublishingService {
    private final ContentService contentService;
    private final PublishingProvider publishingProvider;

    public SocialPublishingService(ContentService contentService,
                                   PublishingProvider publishingProvider) {
        this.contentService = contentService;
        this.publishingProvider = publishingProvider;
    }

    public Optional<Content> publish(Long contentId, Long changedByUserId) {
        Optional<Content> existingContent = contentService.findById(contentId);
        if (existingContent.isEmpty()) {
            return Optional.empty();
        }

        Content content = existingContent.get();
        if (content.getStatus() != ContentStatus.APPROVED) {
            throw new IllegalStateException("Only approved content can be published");
        }

        PublishingResult result = publishingProvider.publish(toRequest(content));
        if (result == null || !result.isSuccess()) {
            throw new IllegalStateException("Publishing provider did not publish the content");
        }

        return contentService.publish(contentId, changedByUserId);
    }

    private PublishingRequest toRequest(Content content) {
        return new PublishingRequest(
                "content:" + content.getContent_id(),
                content.getContent_id(),
                content.getClientId(),
                content.getTitle(),
                content.getDescription(),
                content.getContent_type(),
                content.getFile_url(),
                content.getPlannedPublishDate());
    }
}
