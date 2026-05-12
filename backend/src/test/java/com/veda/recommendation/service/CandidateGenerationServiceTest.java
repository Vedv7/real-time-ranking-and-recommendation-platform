package com.veda.recommendation.service;

import com.veda.recommendation.entity.Content;
import com.veda.recommendation.entity.UserFeature;
import com.veda.recommendation.enums.ContentCategory;
import com.veda.recommendation.repository.ContentFeatureRepository;
import com.veda.recommendation.repository.ContentRepository;
import com.veda.recommendation.repository.InteractionEventRepository;
import com.veda.recommendation.repository.UserFeatureRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CandidateGenerationServiceTest {
    private final ContentRepository contentRepository = mock(ContentRepository.class);
    private final ContentFeatureRepository contentFeatureRepository = mock(ContentFeatureRepository.class);
    private final UserFeatureRepository userFeatureRepository = mock(UserFeatureRepository.class);
    private final InteractionEventRepository interactionEventRepository = mock(InteractionEventRepository.class);
    private final CandidateGenerationService service = new CandidateGenerationService(
            contentRepository,
            contentFeatureRepository,
            userFeatureRepository,
            interactionEventRepository,
            30
    );

    @Test
    void excludesContentTheUserAlreadyInteractedWith() {
        Content seen = content(1L, ContentCategory.TECHNOLOGY);
        Content unseen = content(2L, ContentCategory.EDUCATION);
        when(interactionEventRepository.findInteractedContentIds(7L)).thenReturn(Set.of(1L));
        when(contentRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(seen, unseen));
        when(contentFeatureRepository.findAllByOrderByPopularityScoreDesc(any(Pageable.class))).thenReturn(List.of());
        when(userFeatureRepository.findById(7L)).thenReturn(Optional.empty());
        when(contentRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(seen, unseen)));

        List<Content> candidates = service.generateCandidates(7L);

        assertThat(candidates).extracting(Content::getId).containsExactly(2L);
    }

    @Test
    void addsCandidatesFromPreferredCategories() {
        UserFeature userFeature = new UserFeature();
        userFeature.setUserId(9L);
        userFeature.setPreferredCategories(Set.of(ContentCategory.FINANCE));
        Content finance = content(5L, ContentCategory.FINANCE);

        when(interactionEventRepository.findInteractedContentIds(9L)).thenReturn(Set.of());
        when(contentRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(contentFeatureRepository.findAllByOrderByPopularityScoreDesc(any(Pageable.class))).thenReturn(List.of());
        when(userFeatureRepository.findById(9L)).thenReturn(Optional.of(userFeature));
        when(contentRepository.findByCategoryIn(eq(Set.of(ContentCategory.FINANCE)), any(Pageable.class)))
                .thenReturn(List.of(finance));
        when(contentRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        List<Content> candidates = service.generateCandidates(9L);

        assertThat(candidates).extracting(Content::getId).containsExactly(5L);
    }

    private Content content(Long id, ContentCategory category) {
        Content content = new Content();
        content.setId(id);
        content.setCreatorId(100L + id);
        content.setTitle("Content " + id);
        content.setCategory(category);
        content.setCreatedAt(LocalDateTime.now().minusHours(id));
        content.setDurationSeconds(60);
        content.setLanguage("en");
        content.setQualityScore(0.8);
        return content;
    }
}
