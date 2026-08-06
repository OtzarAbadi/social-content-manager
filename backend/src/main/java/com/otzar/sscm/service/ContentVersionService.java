package com.otzar.sscm.service;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.entities.ContentVersion;
import com.otzar.sscm.entities.ContentVersionChangeType;
import com.otzar.sscm.models.ContentVersionResponse;
import com.otzar.sscm.repository.ContentVersionRepository;
import com.otzar.sscm.repository.ContentMediaRepository;
import com.otzar.sscm.repository.ContentVersionMediaRepository;
import com.otzar.sscm.entities.ContentMedia;
import com.otzar.sscm.entities.ContentVersionMedia;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContentVersionService {

    private final ContentVersionRepository contentVersionRepository;
    private final ContentMediaRepository contentMediaRepository;
    private final ContentVersionMediaRepository versionMediaRepository;

    public ContentVersionService(ContentVersionRepository contentVersionRepository,
                                 ContentMediaRepository contentMediaRepository,
                                 ContentVersionMediaRepository versionMediaRepository) {
        this.contentVersionRepository = contentVersionRepository;
        this.contentMediaRepository = contentMediaRepository;
        this.versionMediaRepository = versionMediaRepository;
    }

    public ContentState capture(Content content) {
        return new ContentState(content);
    }

    public void lockContent(Long contentId) {
        contentVersionRepository.lockContent(contentId);
    }

    public Optional<ContentVersion> findVersion(Long contentId, Integer versionNumber) {
        return contentVersionRepository.findByContentIdAndVersionNumber(contentId, versionNumber).map(this::enrich);
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
        ContentVersion saved = contentVersionRepository.save(version);
        for (ContentMedia media : contentMediaRepository.findByContentId(content.getContent_id())) {
            ContentVersionMedia snapshot = new ContentVersionMedia();
            snapshot.setContentVersionId(saved.getContentVersionId()); snapshot.setMediaUrl(media.getMediaUrl());
            snapshot.setMediaType(media.getMediaType()); snapshot.setDisplayOrder(media.getDisplayOrder()); snapshot.setThumbnailUrl(media.getThumbnailUrl());
            versionMediaRepository.save(snapshot);
        }
        return enrich(saved);
    }

    public List<ContentVersionResponse> findHistory(Long contentId) {
        // Public history is deterministic: version 1 first, newest version last.
        return contentVersionRepository.findByContentIdOrdered(contentId).stream()
                .map(this::enrich).map(ContentVersionResponse::new)
                .collect(Collectors.toList());
    }

    private ContentVersion enrich(ContentVersion version){version.setMedia(versionMediaRepository.findByVersionId(version.getContentVersionId()));return version;}

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
