package com.veda.recommendation.dto;

public record PlatformStatsDto(
        long users,
        long contentItems,
        long interactionEvents,
        long userFeatures,
        long contentFeatures,
        long recommendationLogs
) {
}
