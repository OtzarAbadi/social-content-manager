package com.otzar.sscm.service;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.models.PublishingRequest;
import com.otzar.sscm.models.PublishingResult;
import com.otzar.sscm.entities.PublicationTriggerType;
import org.springframework.stereotype.Service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class SocialPublishingService {
    private final ContentService contentService;
    private final PublishingProvider publishingProvider;
    private final PublicationTransactionService transactions;

    public SocialPublishingService(ContentService contentService, PublishingProvider publishingProvider) {
        this(contentService, publishingProvider, null);
    }

    @Autowired
    public SocialPublishingService(ContentService contentService, PublishingProvider publishingProvider,
                                   PublicationTransactionService transactions) {
        this.contentService = contentService;
        this.publishingProvider = publishingProvider;
        this.transactions = transactions;
    }

    public Optional<Content> publish(Long contentId, Long changedByUserId) {
        return publish(contentId, changedByUserId, PublicationTriggerType.MANUAL);
    }

    public Optional<Content> publish(Long contentId, Long changedByUserId, PublicationTriggerType trigger) {
        if (transactions == null) {
            Optional<Content> existing = contentService.findById(contentId);
            if (existing.isEmpty()) return Optional.empty();
            if (existing.get().getStatus() != ContentStatus.APPROVED) {
                throw new IllegalStateException("Only approved content can be published");
            }
            PublishingResult legacyResult = publishingProvider.publish(
                    toRequest(existing.get(), "content:" + contentId));
            if (legacyResult == null || !legacyResult.isSuccess()) {
                throw new IllegalStateException("Publishing provider did not publish the content");
            }
            return contentService.publish(contentId, changedByUserId);
        }
        Optional<PublicationTransactionService.Claim> claimed =
                transactions.claim(contentId, changedByUserId, trigger, publishingProvider.getType());
        if (claimed.isEmpty()) return Optional.empty();
        PublicationTransactionService.Claim claim = claimed.get();
        PublishingResult result = null;
        try {
            result = publishingProvider.publish(toRequest(claim.content, claim.deliveryKey));
            if (result == null || !result.isSuccess()) {
                transactions.fail(claim.publicationId, result, null);
                throw new IllegalStateException("Publishing provider did not publish the content");
            }
            return transactions.succeed(claim.publicationId, changedByUserId, result);
        } catch (RuntimeException ex) {
            transactions.fail(claim.publicationId, result, ex);
            throw ex;
        }
    }

    private PublishingRequest toRequest(Content content, String deliveryKey) {
        return new PublishingRequest(
                deliveryKey,
                content.getContent_id(),
                content.getClientId(),
                content.getTitle(),
                content.getDescription(),
                content.getContent_type(),
                content.getFile_url(),
                content.getPlannedPublishDate());
    }
}
