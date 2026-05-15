package com.otzar.sscm.controller;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.repository.ContentRepository;
import org.springframework.web.bind.annotation.*;
import com.otzar.sscm.entities.ContentStatus;

import java.util.List;

@RestController
@RequestMapping("/contents")
public class ContentController {

    private final ContentRepository contentRepository;

    public ContentController(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @GetMapping
    public List<Content> getAllContents() {
        return contentRepository.findAll();
    }

    @GetMapping("/by-client")
    public List<Content> getContentsByClient(@RequestParam Long clientId) {
        return contentRepository.findByClientId(clientId);
    }

    @PostMapping
    public Content addContent(@RequestBody Content content) {
        if (content.getStatus() == null) {
            content.setStatus(ContentStatus.DRAFT);        }
        return contentRepository.save(content);
    }

    @PutMapping("/{id}/status")
    public Content updateStatus(@PathVariable Long id, @RequestParam String status) {

        Content content = contentRepository.findById(id).orElseThrow();

        ContentStatus newStatus = ContentStatus.valueOf(status);

        // חוקים
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