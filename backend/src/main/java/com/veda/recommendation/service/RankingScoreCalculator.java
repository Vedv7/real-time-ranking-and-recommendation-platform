package com.veda.recommendation.service;

import com.veda.recommendation.dto.RankingFeatureDto;
import com.veda.recommendation.enums.RankingPolicy;
import org.springframework.stereotype.Component;

@Component
public class RankingScoreCalculator {
    public double score(
            RankingPolicy policy,
            double predictedCtr,
            RankingFeatureDto features,
            double explorationScore,
            double diversityPenalty
    ) {
        double base = 0.65 * predictedCtr
                + 0.15 * features.contentFreshnessScore()
                + 0.10 * features.contentPopularityScore()
                + 0.10 * features.categoryMatch();
        return switch (policy) {
            case FRESHNESS_BOOST -> base + 0.10 * features.contentFreshnessScore();
            case DIVERSITY_BOOST -> base - diversityPenalty;
            case EXPLORATION_BOOST -> base + 0.08 * explorationScore;
            case CONTROL -> base;
        };
    }
}
