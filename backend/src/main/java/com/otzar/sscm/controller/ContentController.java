package com.otzar.sscm.controller;

import com.otzar.sscm.entities.Content;
import com.otzar.sscm.service.ContentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/by-client")
    public List<Content> getContentsByClient(@RequestParam Long clientId) {
        return contentService.findByClientId(clientId);
    }

    @PostMapping
    public Content addContent(@RequestBody Content content) {
        return contentService.create(content);
    }

    @PutMapping("/{id}/status")
    public Content updateStatus(@PathVariable Long id, @RequestParam String status) {
        return contentService.updateStatus(id, status);
    }
}
