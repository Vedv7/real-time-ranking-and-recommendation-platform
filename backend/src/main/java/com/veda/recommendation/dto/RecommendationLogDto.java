package com.veda.recommendation.dto;

import java.time.LocalDateTime;

public record RecommendationLogDto(
        Long id,
        Long userId,
        Long contentId,
        String title,
        Double predictedCtr,
        Double finalScore,
        Integer rankPosition,
        String modelVersion,
        String rankingPolicy,
        String experimentBucket,
        Long latencyMs,
        LocalDateTime createdAt
) {
}
