package com.veda.recommendation.service;

import com.veda.recommendation.dto.PlatformStatsDto;
import com.veda.recommendation.repository.ContentFeatureRepository;
import com.veda.recommendation.repository.ContentRepository;
import com.veda.recommendation.repository.InteractionEventRepository;
import com.veda.recommendation.repository.RecommendationLogRepository;
import com.veda.recommendation.repository.UserFeatureRepository;
import com.veda.recommendation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformStatsService {
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final InteractionEventRepository interactionEventRepository;
    private final UserFeatureRepository userFeatureRepository;
    private final ContentFeatureRepository contentFeatureRepository;
    private final RecommendationLogRepository recommendationLogRepository;

    public PlatformStatsService(
            UserRepository userRepository,
            ContentRepository contentRepository,
            InteractionEventRepository interactionEventRepository,
            UserFeatureRepository userFeatureRepository,
            ContentFeatureRepository contentFeatureRepository,
            RecommendationLogRepository recommendationLogRepository
    ) {
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.interactionEventRepository = interactionEventRepository;
        this.userFeatureRepository = userFeatureRepository;
        this.contentFeatureRepository = contentFeatureRepository;
        this.recommendationLogRepository = recommendationLogRepository;
    }

    @Transactional(readOnly = true)
    public PlatformStatsDto snapshot() {
        return new PlatformStatsDto(
                userRepository.count(),
                contentRepository.count(),
                interactionEventRepository.count(),
                userFeatureRepository.count(),
                contentFeatureRepository.count(),
                recommendationLogRepository.count()
        );
    }
}
