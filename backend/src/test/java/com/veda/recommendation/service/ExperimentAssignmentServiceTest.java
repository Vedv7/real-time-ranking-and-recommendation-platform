package com.veda.recommendation.service;

import com.veda.recommendation.dto.ExperimentAssignmentDto;
import com.veda.recommendation.enums.RankingPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExperimentAssignmentServiceTest {
    private final ExperimentAssignmentService service = new ExperimentAssignmentService();

    @Test
    void assignsUsersDeterministically() {
        ExperimentAssignmentDto first = service.assign(42L);
        ExperimentAssignmentDto second = service.assign(42L);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void coversAllConfiguredRankingPolicies() {
        assertThat(service.assign(1L).rankingPolicy()).isEqualTo(RankingPolicy.CONTROL);
        assertThat(service.assign(55L).rankingPolicy()).isEqualTo(RankingPolicy.FRESHNESS_BOOST);
        assertThat(service.assign(75L).rankingPolicy()).isEqualTo(RankingPolicy.DIVERSITY_BOOST);
        assertThat(service.assign(95L).rankingPolicy()).isEqualTo(RankingPolicy.EXPLORATION_BOOST);
    }
}
