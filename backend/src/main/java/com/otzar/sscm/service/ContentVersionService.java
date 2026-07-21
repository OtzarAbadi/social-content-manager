package com.otzar.sscm.service;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.entities.ContentVersion;
import com.otzar.sscm.entities.ContentVersionChangeType;
import com.otzar.sscm.models.ContentVersionResponse;
import com.otzar.sscm.repository.ContentVersionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ContentVersionService {

    private final ContentVersionRepository contentVersionRepository;

    public ContentVersionService(ContentVersionRepository contentVersionRepository) {
        this.contentVersionRepository = contentVersionRepository;
    }

    public ContentState capture(Content content) {
        return new ContentState(content);
    }

    public boolean hasMeaningfulChanges(ContentState before, Content after) {
        return !before.matches(after);
    }

    public ContentVersion createSnapshot(Content content, Long changedByUserId,
                                         ContentVersionChangeType changeType) {
        contentVersionRepository.lockContent(content.getContent_id());

        ContentVersion version = new ContentVersion();
        version.setContentId(content.getContent_id());
        version.setVersionNumber(contentVersionRepository.nextVersionNumber(content.getContent_id()));
        version.setTitle(content.getTitle());
        version.setDescription(content.getDescription());
        version.setContentType(content.getContent_type());
        version.setFileUrl(content.getFile_url());
        version.setStatus(content.getStatus());
        version.setPlannedPublishDate(content.getPlannedPublishDate());
        version.setChangedByUserId(changedByUserId);
        version.setChangedAt(LocalDateTime.now());
        version.setChangeType(changeType);
        return contentVersionRepository.save(version);
    }

    public List<ContentVersionResponse> findHistory(Long contentId) {
        // Public history is deterministic: version 1 first, newest version last.
        return contentVersionRepository.findByContentIdOrdered(contentId).stream()
                .map(ContentVersionResponse::new)
                .collect(Collectors.toList());
    }

    public static final class ContentState {
        private final Long clientId;
        private final String title;
        private final String description;
        private final String fileUrl;
        private final String contentType;
        private final ContentStatus status;
        private final LocalDateTime plannedPublishDate;

        private ContentState(Content content) {
            this.clientId = content.getClientId();
            this.title = content.getTitle();
            this.description = content.getDescription();
            this.fileUrl = content.getFile_url();
            this.contentType = content.getContent_type();
            this.status = content.getStatus();
            this.plannedPublishDate = content.getPlannedPublishDate();
        }

        private boolean matches(Content content) {
            return Objects.equals(clientId, content.getClientId())
                    && Objects.equals(title, content.getTitle())
                    && Objects.equals(description, content.getDescription())
                    && Objects.equals(fileUrl, content.getFile_url())
                    && Objects.equals(contentType, content.getContent_type())
                    && status == content.getStatus()
                    && Objects.equals(plannedPublishDate, content.getPlannedPublishDate());
        }
    }
}
