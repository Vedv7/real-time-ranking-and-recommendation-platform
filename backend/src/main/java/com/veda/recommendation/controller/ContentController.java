package com.veda.recommendation.controller;

import com.veda.recommendation.dto.CreateContentRequest;
import com.veda.recommendation.entity.Content;
import com.veda.recommendation.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content")
public class ContentController {
    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Content create(@Valid @RequestBody CreateContentRequest request) {
        return contentService.create(request);
    }

    @GetMapping
    public List<Content> list() {
        return contentService.list();
    }

    @GetMapping("/{id}")
    public Content get(@PathVariable Long id) {
        return contentService.get(id);
    }
}
