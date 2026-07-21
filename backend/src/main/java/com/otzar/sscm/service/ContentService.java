package com.otzar.sscm.service;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.entities.ContentVersionChangeType;
import com.otzar.sscm.entities.ContentVersion;
import com.otzar.sscm.entities.Comment;
import com.otzar.sscm.models.RestoreContentVersionResponse;
import com.otzar.sscm.repository.ClientRepository;
import com.otzar.sscm.repository.CommentRepository;
import com.otzar.sscm.repository.ContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ContentService {

    private static final Logger logger = LoggerFactory.getLogger(ContentService.class);
    private final ContentRepository contentRepository;
    private final ClientRepository clientRepository;
    private final CommentRepository commentRepository;
    private final ContentVersionService contentVersionService;
    private final FileStorageService fileStorageService;

    public ContentService(ContentRepository contentRepository, ClientRepository clientRepository,
                          CommentRepository commentRepository, ContentVersionService contentVersionService,
                          FileStorageService fileStorageService) {
        this.contentRepository = contentRepository;
        this.clientRepository = clientRepository;
        this.commentRepository = commentRepository;
        this.contentVersionService = contentVersionService;
        this.fileStorageService = fileStorageService;
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

    public List<Content> findByClientIdAndStatus(Long clientId, ContentStatus status) {
        return contentRepository.findByClientIdAndStatus(clientId, status);
    }

    public Optional<Content> findById(Long id) {
        return contentRepository.findById(id);
    }

    @Transactional
    public RestoreContentVersionResult restoreVersion(Long contentId, Integer versionNumber,
                                                      Long changedByUserId) {
        contentVersionService.lockContent(contentId);
        Optional<Content> existingContent = contentRepository.findById(contentId);
        if (existingContent.isEmpty()) {
            return RestoreContentVersionResult.contentNotFound();
        }

        Optional<ContentVersion> sourceVersion = contentVersionService.findVersion(contentId, versionNumber);
        if (sourceVersion.isEmpty()) {
            return RestoreContentVersionResult.versionNotFound();
        }

        Content content = existingContent.get();
        if (content.getStatus() != ContentStatus.DRAFT && content.getStatus() != ContentStatus.REJECTED) {
            throw new IllegalStateException("Content cannot be restored in its current status");
        }

        ContentVersion source = sourceVersion.get();
        if (!fileStorageService.isManagedUploadAvailable(source.getFileUrl())) {
            throw new IllegalStateException("Historical media file is unavailable");
        }

        boolean changed = !java.util.Objects.equals(content.getTitle(), source.getTitle())
                || !java.util.Objects.equals(content.getDescription(), source.getDescription())
                || !java.util.Objects.equals(content.getContent_type(), source.getContentType())
                || !java.util.Objects.equals(content.getFile_url(), source.getFileUrl());
        if (!changed) {
            return RestoreContentVersionResult.success(new RestoreContentVersionResponse(
                    content, versionNumber, null, false));
        }

        content.setTitle(source.getTitle());
        content.setDescription(source.getDescription());
        content.setContent_type(source.getContentType());
        content.setFile_url(source.getFileUrl());
        Content restored = contentRepository.save(content);
        ContentVersion newVersion = contentVersionService.createSnapshot(
                restored, changedByUserId, ContentVersionChangeType.EDITED);
        return RestoreContentVersionResult.success(new RestoreContentVersionResponse(
                restored, versionNumber, newVersion.getVersionNumber(), true));
    }

    @Transactional
    public ContentOperationResult create(Content content) {
        return create(content, null);
    }

    @Transactional
    public ContentOperationResult create(Content content, Long changedByUserId) {
        if (content.getTitle() == null || content.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (!clientExists(content.getClientId())) {
            return ContentOperationResult.clientNotFound();
        }

        if (content.getStatus() != null && content.getStatus() != ContentStatus.DRAFT) {
            throw new IllegalStateException("Content must be created as draft");
        }

        content.setStatus(ContentStatus.DRAFT);
        Content created = contentRepository.save(content);
        contentVersionService.createSnapshot(created, changedByUserId, ContentVersionChangeType.CREATED);
        return ContentOperationResult.success(created);
    }

    @Transactional
    public ContentOperationResult update(Long id, Content request) {
        return update(id, request, null);
    }

    @Transactional
    public ContentOperationResult update(Long id, Content request, Long changedByUserId) {
        Optional<Content> existingContent = contentRepository.findById(id);

        if (existingContent.isEmpty()) {
            return ContentOperationResult.contentNotFound();
        }

        if (request.getClientId() != null && !clientExists(request.getClientId())) {
            return ContentOperationResult.clientNotFound();
        }

        Content content = existingContent.get();
        ContentVersionService.ContentState before = contentVersionService.capture(content);
        applyRequest(content, request);

        if (!contentVersionService.hasMeaningfulChanges(before, content)) {
            return ContentOperationResult.success(content);
        }

        Content updated = contentRepository.save(content);
        contentVersionService.createSnapshot(updated, changedByUserId, ContentVersionChangeType.EDITED);
        return ContentOperationResult.success(updated);
    }

    @Transactional
    public Optional<Content> updatePlannedPublishDate(Long id, LocalDateTime plannedPublishDate) {
        return updatePlannedPublishDate(id, plannedPublishDate, null);
    }

    @Transactional
    public Optional<Content> updatePlannedPublishDate(Long id, LocalDateTime plannedPublishDate,
                                                      Long changedByUserId) {
        return contentRepository.findById(id).map(content -> {
            if (java.util.Objects.equals(content.getPlannedPublishDate(), plannedPublishDate)) {
                return content;
            }
            content.setPlannedPublishDate(plannedPublishDate);
            Content updated = contentRepository.save(content);
            contentVersionService.createSnapshot(updated, changedByUserId, ContentVersionChangeType.SCHEDULED);
            return updated;
        });
    }

    public boolean delete(Long id) {
        Optional<Content> existingContent = contentRepository.findById(id);

        if (existingContent.isEmpty()) {
            return false;
        }

        contentRepository.delete(existingContent.get());
        return true;
    }

    @Transactional
    public Optional<Content> updateStatus(Long id, String status) {
        return updateStatus(id, status, null);
    }

    @Transactional
    public Optional<Content> updateStatus(Long id, String status, Long changedByUserId) {
        return changeStatus(id, ContentStatus.valueOf(status), changedByUserId);
    }

    @Transactional
    public Optional<Content> sendForApproval(Long id) {
        return sendForApproval(id, null);
    }

    @Transactional
    public Optional<Content> sendForApproval(Long id, Long changedByUserId) {
        return changeStatus(id, ContentStatus.WAITING_APPROVAL, changedByUserId);
    }

    @Transactional
    public Optional<Content> approve(Long id) {
        return approve(id, null);
    }

    @Transactional
    public Optional<Content> approve(Long id, Long changedByUserId) {
        return changeStatus(id, ContentStatus.APPROVED, changedByUserId);
    }

    @Transactional
    public Optional<Content> reject(Long id) {
        return reject(id, null);
    }

    @Transactional
    public Optional<Content> reject(Long id, Long changedByUserId) {
        return changeStatus(id, ContentStatus.REJECTED, changedByUserId);
    }

    @Transactional
    public Optional<Content> reject(Long id, Long userId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        Optional<Content> rejectedContent = changeStatus(id, ContentStatus.REJECTED, userId);
        rejectedContent.ifPresent(content -> {
            Comment comment = new Comment();
            comment.setContentId(id);
            comment.setUserId(userId);
            comment.setCommentText(reason.trim());
            commentRepository.save(comment);
        });
        return rejectedContent;
    }

    @Transactional
    public Optional<Content> publish(Long id) {
        return publish(id, null);
    }

    @Transactional
    public Optional<Content> publish(Long id, Long changedByUserId) {
        return changeStatus(id, ContentStatus.PUBLISHED, changedByUserId);
    }

    private Optional<Content> changeStatus(Long id, ContentStatus newStatus, Long changedByUserId) {
        Optional<Content> existingContent = contentRepository.findById(id);

        if (existingContent.isEmpty()) {
            return Optional.empty();
        }

        Content content = existingContent.get();
        validateStatusTransition(content.getStatus(), newStatus);

        if (content.getStatus() == newStatus) {
            return Optional.of(content);
        }

        content.setStatus(newStatus);
        Content updated = contentRepository.save(content);
        contentVersionService.createSnapshot(updated, changedByUserId, ContentVersionChangeType.STATUS_CHANGED);
        return Optional.of(updated);
    }

    private void validateStatusTransition(ContentStatus currentStatus, ContentStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (newStatus == ContentStatus.WAITING_APPROVAL) {
            if (currentStatus != ContentStatus.DRAFT && currentStatus != ContentStatus.REJECTED) {
                throw new IllegalStateException("Only draft or rejected content can be sent for approval");
            }

            return;
        }

        if (newStatus == ContentStatus.APPROVED || newStatus == ContentStatus.REJECTED) {
            if (currentStatus != ContentStatus.WAITING_APPROVAL) {
                throw new IllegalStateException("Only content waiting for approval can be approved/rejected");
            }

            return;
        }

        if (newStatus == ContentStatus.PUBLISHED) {
            if (currentStatus != ContentStatus.APPROVED) {
                throw new IllegalStateException("Only approved content can be published");
            }

            return;
        }

        throw new IllegalStateException("Unsupported content status transition");
    }

    public boolean clientExists(Long clientId) {
        boolean exists = clientId != null && clientRepository.findById(clientId).isPresent();
        logger.info("Client lookup for content creation: clientId={}, found={}", clientId, exists);
        return exists;
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

    public static class RestoreContentVersionResult {
        private final RestoreContentVersionResponse response;
        private final RestoreFailureReason failureReason;

        private RestoreContentVersionResult(RestoreContentVersionResponse response,
                                            RestoreFailureReason failureReason) {
            this.response = response;
            this.failureReason = failureReason;
        }

        public static RestoreContentVersionResult success(RestoreContentVersionResponse response) {
            return new RestoreContentVersionResult(response, null);
        }

        public static RestoreContentVersionResult contentNotFound() {
            return new RestoreContentVersionResult(null, RestoreFailureReason.CONTENT_NOT_FOUND);
        }

        public static RestoreContentVersionResult versionNotFound() {
            return new RestoreContentVersionResult(null, RestoreFailureReason.VERSION_NOT_FOUND);
        }

        public boolean isSuccess() { return response != null; }
        public RestoreContentVersionResponse getResponse() { return response; }
        public RestoreFailureReason getFailureReason() { return failureReason; }
    }

    public enum RestoreFailureReason {
        CONTENT_NOT_FOUND,
        VERSION_NOT_FOUND
    }
}
