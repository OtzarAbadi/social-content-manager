package com.otzar.sscm.controller;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.entities.ContentStatus;
import com.otzar.sscm.service.ContentService;
import com.otzar.sscm.service.ContentService.ContentOperationResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/contents")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public List<Content> getAllContents() {
        return contentService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Content> getContentById(@PathVariable Long id) {
        return contentService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-client")
    public List<Content> getContentsByClient(@RequestParam Long clientId) {
        return contentService.findByClientId(clientId);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Content>> getContentsByClientId(@PathVariable Long clientId) {
        return contentService.findByClientIdIfClientExists(clientId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Content>> getContentsByStatus(@PathVariable String status) {
        try {
            return ResponseEntity.ok(contentService.findByStatus(parseStatus(status)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Content> addContent(@RequestBody Content content) {
        ContentOperationResult result = contentService.create(content);

        if (!result.isSuccess()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result.getContent());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Content> updateContent(@PathVariable Long id, @RequestBody Content content) {
        ContentOperationResult result = contentService.update(id, content);

        if (!result.isSuccess()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result.getContent());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable Long id) {
        if (!contentService.delete(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Content> updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            return contentService.updateStatus(id, parseStatus(status).name())
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    private ContentStatus parseStatus(String status) {
        return ContentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
    }
}
