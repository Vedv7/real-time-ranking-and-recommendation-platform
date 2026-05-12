package com.veda.recommendation.service;

import com.veda.recommendation.dto.FeatureStoreStatusDto;
import com.veda.recommendation.repository.ContentFeatureRepository;
import com.veda.recommendation.repository.UserFeatureRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FeatureStoreDiagnosticsService {
    private static final long STALE_THRESHOLD_MINUTES = 60;

    private final UserFeatureRepository userFeatureRepository;
    private final ContentFeatureRepository contentFeatureRepository;

    public FeatureStoreDiagnosticsService(
            UserFeatureRepository userFeatureRepository,
            ContentFeatureRepository contentFeatureRepository
    ) {
        this.userFeatureRepository = userFeatureRepository;
        this.contentFeatureRepository = contentFeatureRepository;
    }

    public FeatureStoreStatusDto status() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(STALE_THRESHOLD_MINUTES);
        return new FeatureStoreStatusDto(
                userFeatureRepository.count(),
                contentFeatureRepository.count(),
                userFeatureRepository.countByUpdatedAtBefore(threshold),
                contentFeatureRepository.countByUpdatedAtBefore(threshold),
                STALE_THRESHOLD_MINUTES
        );
    }
}
