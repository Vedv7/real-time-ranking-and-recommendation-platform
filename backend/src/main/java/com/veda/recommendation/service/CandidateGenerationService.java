package com.veda.recommendation.service;

import com.veda.recommendation.entity.Content;
import com.veda.recommendation.entity.ContentFeature;
import com.veda.recommendation.entity.UserFeature;
import com.veda.recommendation.repository.ContentFeatureRepository;
import com.veda.recommendation.repository.ContentRepository;
import com.veda.recommendation.repository.InteractionEventRepository;
import com.veda.recommendation.repository.UserFeatureRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CandidateGenerationService {
    private final ContentRepository contentRepository;
    private final ContentFeatureRepository contentFeatureRepository;
    private final UserFeatureRepository userFeatureRepository;
    private final InteractionEventRepository interactionEventRepository;
    private final int candidateWindowDays;

    public CandidateGenerationService(
            ContentRepository contentRepository,
            ContentFeatureRepository contentFeatureRepository,
            UserFeatureRepository userFeatureRepository,
            InteractionEventRepository interactionEventRepository,
            @Value("${recommendation.candidate-window-days}") int candidateWindowDays
    ) {
        this.contentRepository = contentRepository;
        this.contentFeatureRepository = contentFeatureRepository;
        this.userFeatureRepository = userFeatureRepository;
        this.interactionEventRepository = interactionEventRepository;
        this.candidateWindowDays = candidateWindowDays;
    }

    @Transactional(readOnly = true)
    public List<Content> generateCandidates(Long userId) {
        Set<Long> seenContentIds = interactionEventRepository.findInteractedContentIds(userId);
        Map<Long, Content> candidates = new LinkedHashMap<>();

        contentRepository.findByCreatedAtAfterOrderByCreatedAtDesc(
                LocalDateTime.now().minusDays(candidateWindowDays),
                PageRequest.of(0, 100)
        ).forEach(content -> candidates.put(content.getId(), content));

        contentFeatureRepository.findAllByOrderByPopularityScoreDesc(PageRequest.of(0, 100))
                .stream()
                .map(ContentFeature::getContentId)
                .forEach(id -> contentRepository.findById(id).ifPresent(content -> candidates.put(content.getId(), content)));

        userFeatureRepository.findById(userId)
                .map(UserFeature::getPreferredCategories)
                .filter(categories -> !categories.isEmpty())
                .ifPresent(categories -> contentRepository.findByCategoryIn(categories, PageRequest.of(0, 100))
                        .forEach(content -> candidates.put(content.getId(), content)));

        if (candidates.size() < 100) {
            contentRepository.findAll(PageRequest.of(0, 100)).forEach(content -> candidates.put(content.getId(), content));
        }

        return candidates.values().stream()
                .filter(content -> !seenContentIds.contains(content.getId()))
                .limit(100)
                .toList();
    }
}
