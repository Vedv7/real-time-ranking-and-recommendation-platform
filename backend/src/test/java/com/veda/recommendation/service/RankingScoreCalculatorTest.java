package com.veda.recommendation.service;

import com.veda.recommendation.dto.RankingFeatureDto;
import com.veda.recommendation.enums.RankingPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RankingScoreCalculatorTest {
    private final RankingScoreCalculator calculator = new RankingScoreCalculator();

    @Test
    void controlScoreBlendsCtrFreshnessPopularityAndCategoryMatch() {
        double score = calculator.score(RankingPolicy.CONTROL, 0.8, features(), 0.6, 0.1);

        assertThat(score).isCloseTo(0.80, withinTolerance());
    }

    @Test
    void freshnessPolicyBoostsFreshCandidates() {
        RankingFeatureDto features = features();

        double control = calculator.score(RankingPolicy.CONTROL, 0.8, features, 0.6, 0.1);
        double boosted = calculator.score(RankingPolicy.FRESHNESS_BOOST, 0.8, features, 0.6, 0.1);

        assertThat(boosted).isGreaterThan(control);
        assertThat(boosted - control).isCloseTo(0.08, withinTolerance());
    }

    @Test
    void diversityPolicyPenalizesOverrepresentedCategories() {
        RankingFeatureDto features = features();

        double control = calculator.score(RankingPolicy.CONTROL, 0.8, features, 0.6, 0.12);
        double diversified = calculator.score(RankingPolicy.DIVERSITY_BOOST, 0.8, features, 0.6, 0.12);

        assertThat(control - diversified).isCloseTo(0.12, withinTolerance());
    }

    @Test
    void explorationPolicyBoostsNovelCandidates() {
        RankingFeatureDto features = features();

        double control = calculator.score(RankingPolicy.CONTROL, 0.8, features, 0.75, 0.1);
        double exploration = calculator.score(RankingPolicy.EXPLORATION_BOOST, 0.8, features, 0.75, 0.1);

        assertThat(exploration - control).isCloseTo(0.06, withinTolerance());
    }

    private RankingFeatureDto features() {
        return new RankingFeatureDto(
                120L,
                22.0,
                0.2,
                0.1,
                0.6,
                0.8,
                0.35,
                1,
                0.4,
                12.0
        );
    }

    private org.assertj.core.data.Offset<Double> withinTolerance() {
        return org.assertj.core.data.Offset.offset(0.0001);
    }
}
