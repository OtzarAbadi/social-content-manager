package com.otzar.sscm.service;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.repository.ContentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentService {

    private final ContentRepository contentRepository;

    public ContentService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public List<Content> findAll() {
        return contentRepository.findAll();
    }

    public List<Content> findByClientId(Long clientId) {
        return contentRepository.findByClientId(clientId);
    }

    public Content create(Content content) {
        if (content.getStatus() == null) {
            content.setStatus(ContentStatus.DRAFT);
        }

        return contentRepository.save(content);
    }

    public Content updateStatus(Long id, String status) {
        Content content = contentRepository.findById(id).orElseThrow();
        ContentStatus newStatus = ContentStatus.valueOf(status);

        if (newStatus == ContentStatus.APPROVED || newStatus == ContentStatus.REJECTED) {
            if (content.getStatus() != ContentStatus.WAITING_APPROVAL) {
                throw new RuntimeException("Only content waiting for approval can be approved/rejected");
            }
        }

        if (newStatus == ContentStatus.WAITING_APPROVAL) {
            if (content.getStatus() != ContentStatus.DRAFT) {
                throw new RuntimeException("Only draft can be sent for approval");
            }
        }

        content.setStatus(newStatus);
        return contentRepository.save(content);
    }
}
