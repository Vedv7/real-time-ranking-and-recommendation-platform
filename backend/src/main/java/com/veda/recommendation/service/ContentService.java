package com.veda.recommendation.service;

import com.veda.recommendation.dto.CreateContentRequest;
import com.veda.recommendation.entity.Content;
import com.veda.recommendation.entity.ContentFeature;
import com.veda.recommendation.exception.ResourceNotFoundException;
import com.veda.recommendation.repository.ContentFeatureRepository;
import com.veda.recommendation.repository.ContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContentService {
    private final ContentRepository contentRepository;
    private final ContentFeatureRepository contentFeatureRepository;

    public ContentService(ContentRepository contentRepository, ContentFeatureRepository contentFeatureRepository) {
        this.contentRepository = contentRepository;
        this.contentFeatureRepository = contentFeatureRepository;
    }

    @Transactional
    public Content create(CreateContentRequest request) {
        Content content = new Content();
        content.setCreatorId(request.creatorId());
        content.setTitle(request.title());
        content.setCategory(request.category());
        content.setTags(request.tags() == null ? List.of() : request.tags());
        content.setDurationSeconds(request.durationSeconds());
        content.setLanguage(request.language());
        content.setQualityScore(request.qualityScore());
        Content saved = contentRepository.save(content);

        ContentFeature feature = new ContentFeature();
        feature.setContentId(saved.getId());
        contentFeatureRepository.save(feature);
        return saved;
    }

    public List<Content> list() {
        return contentRepository.findAll();
    }

    public Content get(Long id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found: " + id));
    }
}
