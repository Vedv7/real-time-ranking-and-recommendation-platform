package com.veda.recommendation.dto;

public record FeatureStoreStatusDto(
        long userFeatureCount,
        long contentFeatureCount,
        long staleUserFeatureCount,
        long staleContentFeatureCount,
        long freshnessThresholdMinutes
) {
}
