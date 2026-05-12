package com.veda.recommendation.dto;

import com.veda.recommendation.enums.RankingPolicy;

public record ExperimentAssignmentDto(
        String experimentName,
        String bucket,
        RankingPolicy rankingPolicy
) {
}
