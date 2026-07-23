package com.otzar.sscm.service;

import com.otzar.sscm.entities.Client;
import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.entities.ContentVersion;
import com.otzar.sscm.entities.ContentVersionChangeType;
import com.otzar.sscm.models.ActivityResponse;
import com.otzar.sscm.models.ActivitySource;
import com.otzar.sscm.models.ActivityType;
import com.otzar.sscm.repository.ClientRepository;
import com.otzar.sscm.repository.ContentRepository;
import org.hibernate.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActivityService {
    private final Persist persist;
    private final ContentRepository contentRepository;
    private final ClientRepository clientRepository;

    public ActivityService(Persist persist, ContentRepository contentRepository,
                           ClientRepository clientRepository) {
        this.persist = persist;
        this.contentRepository = contentRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> findActivities(Long clientId, ActivityType requestedType,
                                                 int limit, boolean includeClientName) {
        StringBuilder hql = new StringBuilder("FROM ContentVersion version WHERE 1 = 1");
        if (clientId != null) {
            hql.append(" AND version.contentId IN ")
                    .append("(SELECT content.content_id FROM Content content WHERE content.clientId = :clientId)");
        }
        appendTypeFilter(hql, requestedType);
        hql.append(" ORDER BY version.changedAt DESC, version.contentVersionId DESC");

        Query<ContentVersion> query = persist.getQuerySession().createQuery(hql.toString(), ContentVersion.class);
        if (clientId != null) query.setParameter("clientId", clientId);
        setTypeParameters(query, requestedType);
        query.setMaxResults(limit);

        return query.list().stream()
                .map(version -> toResponse(version, includeClientName))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ActivityResponse toResponse(ContentVersion version, boolean includeClientName) {
        ActivityType type = mapType(version);
        if (type == null) return null;

        Optional<Content> content = contentRepository.findById(version.getContentId());
        if (content.isEmpty()) return null;
        Long clientId = content.get().getClientId();
        String clientName = includeClientName
                ? clientRepository.findById(clientId).map(Client::getBusiness_name).orElse(null)
                : null;

        return new ActivityResponse(
                "VERSION:" + version.getContentVersionId(),
                ActivitySource.CONTENT_VERSION,
                type,
                version.getChangedAt(),
                version.getContentId(),
                version.getTitle(),
                clientId,
                clientName,
                version.getStatus(),
                version.getVersionNumber());
    }

    private ActivityType mapType(ContentVersion version) {
        if (version.getChangeType() == ContentVersionChangeType.CREATED) return ActivityType.CONTENT_CREATED;
        if (version.getChangeType() == ContentVersionChangeType.EDITED) return ActivityType.CONTENT_UPDATED;
        if (version.getChangeType() == ContentVersionChangeType.SCHEDULED) return ActivityType.SCHEDULED;
        if (version.getChangeType() != ContentVersionChangeType.STATUS_CHANGED) return null;
        if (version.getStatus() == ContentStatus.WAITING_APPROVAL) return ActivityType.SENT_FOR_APPROVAL;
        if (version.getStatus() == ContentStatus.APPROVED) return ActivityType.APPROVED;
        if (version.getStatus() == ContentStatus.REJECTED) return ActivityType.REJECTED;
        if (version.getStatus() == ContentStatus.PUBLISHED) return ActivityType.PUBLISHED;
        return null;
    }

    private void appendTypeFilter(StringBuilder hql, ActivityType type) {
        if (type == null) return;
        if (type == ActivityType.CONTENT_CREATED) {
            hql.append(" AND version.changeType = :changeType");
        } else if (type == ActivityType.CONTENT_UPDATED) {
            hql.append(" AND version.changeType = :changeType");
        } else if (type == ActivityType.SCHEDULED) {
            hql.append(" AND version.changeType = :changeType");
        } else {
            hql.append(" AND version.changeType = :statusChangeType AND version.status = :status");
        }
    }

    private void setTypeParameters(Query<ContentVersion> query, ActivityType type) {
        if (type == null) return;
        if (type == ActivityType.CONTENT_CREATED) {
            query.setParameter("changeType", ContentVersionChangeType.CREATED);
        } else if (type == ActivityType.CONTENT_UPDATED) {
            query.setParameter("changeType", ContentVersionChangeType.EDITED);
        } else if (type == ActivityType.SCHEDULED) {
            query.setParameter("changeType", ContentVersionChangeType.SCHEDULED);
        } else {
            query.setParameter("statusChangeType", ContentVersionChangeType.STATUS_CHANGED);
            query.setParameter("status", statusFor(type));
        }
    }

    private ContentStatus statusFor(ActivityType type) {
        if (type == ActivityType.SENT_FOR_APPROVAL) return ContentStatus.WAITING_APPROVAL;
        if (type == ActivityType.APPROVED) return ContentStatus.APPROVED;
        if (type == ActivityType.REJECTED) return ContentStatus.REJECTED;
        if (type == ActivityType.PUBLISHED) return ContentStatus.PUBLISHED;
        throw new IllegalArgumentException("Unsupported activity type");
    }
}
