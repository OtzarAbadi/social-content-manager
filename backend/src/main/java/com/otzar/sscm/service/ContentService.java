package com.otzar.sscm.service;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.repository.ClientRepository;
import com.otzar.sscm.repository.ContentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContentService {

    private final ContentRepository contentRepository;
    private final ClientRepository clientRepository;

    public ContentService(ContentRepository contentRepository, ClientRepository clientRepository) {
        this.contentRepository = contentRepository;
        this.clientRepository = clientRepository;
    }

    public List<Content> findAll() {
        return contentRepository.findAll();
    }

    public List<Content> findByClientId(Long clientId) {
        return contentRepository.findByClientId(clientId);
    }

    public Optional<List<Content>> findByClientIdIfClientExists(Long clientId) {
        if (!clientExists(clientId)) {
            return Optional.empty();
        }

        return Optional.of(contentRepository.findByClientId(clientId));
    }

    public List<Content> findByStatus(ContentStatus status) {
        return contentRepository.findByStatus(status);
    }

    public Optional<Content> findById(Long id) {
        return contentRepository.findById(id);
    }

    public ContentOperationResult create(Content content) {
        if (!clientExists(content.getClientId())) {
            return ContentOperationResult.clientNotFound();
        }

        if (content.getStatus() == null) {
            content.setStatus(ContentStatus.DRAFT);
        }

        return ContentOperationResult.success(contentRepository.save(content));
    }

    public ContentOperationResult update(Long id, Content request) {
        Optional<Content> existingContent = contentRepository.findById(id);

        if (existingContent.isEmpty()) {
            return ContentOperationResult.contentNotFound();
        }

        if (request.getClientId() != null && !clientExists(request.getClientId())) {
            return ContentOperationResult.clientNotFound();
        }

        Content content = existingContent.get();
        applyRequest(content, request);

        return ContentOperationResult.success(contentRepository.save(content));
    }

    public boolean delete(Long id) {
        Optional<Content> existingContent = contentRepository.findById(id);

        if (existingContent.isEmpty()) {
            return false;
        }

        contentRepository.delete(existingContent.get());
        return true;
    }

    public Optional<Content> updateStatus(Long id, String status) {
        Optional<Content> existingContent = contentRepository.findById(id);

        if (existingContent.isEmpty()) {
            return Optional.empty();
        }

        Content content = existingContent.get();
        ContentStatus newStatus = ContentStatus.valueOf(status);

        if (newStatus == ContentStatus.APPROVED || newStatus == ContentStatus.REJECTED) {
            if (content.getStatus() != ContentStatus.WAITING_APPROVAL) {
                throw new IllegalStateException("Only content waiting for approval can be approved/rejected");
            }
        }

        if (newStatus == ContentStatus.WAITING_APPROVAL) {
            if (content.getStatus() != ContentStatus.DRAFT) {
                throw new IllegalStateException("Only draft can be sent for approval");
            }
        }

        content.setStatus(newStatus);
        return Optional.of(contentRepository.save(content));
    }

    private boolean clientExists(Long clientId) {
        return clientId != null && clientRepository.findById(clientId).isPresent();
    }

    private void applyRequest(Content content, Content request) {
        if (request.getTitle() != null) {
            content.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            content.setDescription(request.getDescription());
        }

        if (request.getFile_url() != null) {
            content.setFile_url(request.getFile_url());
        }

        if (request.getContent_type() != null) {
            content.setContent_type(request.getContent_type());
        }

        if (request.getStatus() != null) {
            content.setStatus(request.getStatus());
        }

        if (request.getClientId() != null) {
            content.setClientId(request.getClientId());
        }

        if (request.getPlannedPublishDate() != null) {
            content.setPlannedPublishDate(request.getPlannedPublishDate());
        }
    }

    public static class ContentOperationResult {
        private final Content content;
        private final FailureReason failureReason;

        private ContentOperationResult(Content content, FailureReason failureReason) {
            this.content = content;
            this.failureReason = failureReason;
        }

        public static ContentOperationResult success(Content content) {
            return new ContentOperationResult(content, null);
        }

        public static ContentOperationResult contentNotFound() {
            return new ContentOperationResult(null, FailureReason.CONTENT_NOT_FOUND);
        }

        public static ContentOperationResult clientNotFound() {
            return new ContentOperationResult(null, FailureReason.CLIENT_NOT_FOUND);
        }

        public boolean isSuccess() {
            return content != null;
        }

        public Content getContent() {
            return content;
        }

        public FailureReason getFailureReason() {
            return failureReason;
        }
    }

    public enum FailureReason {
        CONTENT_NOT_FOUND,
        CLIENT_NOT_FOUND
    }
}
