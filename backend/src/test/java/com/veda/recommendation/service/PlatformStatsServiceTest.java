package com.veda.recommendation.service;

import com.veda.recommendation.dto.PlatformStatsDto;
import com.veda.recommendation.repository.ContentFeatureRepository;
import com.veda.recommendation.repository.ContentRepository;
import com.veda.recommendation.repository.InteractionEventRepository;
import com.veda.recommendation.repository.RecommendationLogRepository;
import com.veda.recommendation.repository.UserFeatureRepository;
import com.veda.recommendation.repository.UserRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformStatsServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ContentRepository contentRepository = mock(ContentRepository.class);
    private final InteractionEventRepository interactionEventRepository = mock(InteractionEventRepository.class);
    private final UserFeatureRepository userFeatureRepository = mock(UserFeatureRepository.class);
    private final ContentFeatureRepository contentFeatureRepository = mock(ContentFeatureRepository.class);
    private final RecommendationLogRepository recommendationLogRepository = mock(RecommendationLogRepository.class);
    private final PlatformStatsService service = new PlatformStatsService(
            userRepository,
            contentRepository,
            interactionEventRepository,
            userFeatureRepository,
            contentFeatureRepository,
            recommendationLogRepository
    );

    @Test
    void returnsCountsAcrossRuntimeStores() {
        when(userRepository.count()).thenReturn(3L);
        when(contentRepository.count()).thenReturn(12L);
        when(interactionEventRepository.count()).thenReturn(42L);
        when(userFeatureRepository.count()).thenReturn(2L);
        when(contentFeatureRepository.count()).thenReturn(10L);
        when(recommendationLogRepository.count()).thenReturn(30L);

        PlatformStatsDto stats = service.snapshot();

        assertThat(stats.users()).isEqualTo(3L);
        assertThat(stats.contentItems()).isEqualTo(12L);
        assertThat(stats.interactionEvents()).isEqualTo(42L);
        assertThat(stats.userFeatures()).isEqualTo(2L);
        assertThat(stats.contentFeatures()).isEqualTo(10L);
        assertThat(stats.recommendationLogs()).isEqualTo(30L);
    }
}
