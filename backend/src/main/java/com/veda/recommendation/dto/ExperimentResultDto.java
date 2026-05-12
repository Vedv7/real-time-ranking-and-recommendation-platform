package com.veda.recommendation.dto;

public record ExperimentResultDto(
        String experimentBucket,
        String rankingPolicy,
        long impressions,
        double avgPredictedCtr,
        double avgFinalScore,
        double avgLatencyMs,
        double topRankShare
) {
}
