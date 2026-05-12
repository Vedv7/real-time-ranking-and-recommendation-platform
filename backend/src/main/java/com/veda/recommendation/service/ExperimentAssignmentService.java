package com.veda.recommendation.service;

import com.veda.recommendation.dto.ExperimentAssignmentDto;
import com.veda.recommendation.enums.RankingPolicy;
import org.springframework.stereotype.Service;

@Service
public class ExperimentAssignmentService {
    public ExperimentAssignmentDto assign(Long userId) {
        int bucket = Math.floorMod(userId.hashCode(), 100);
        if (bucket < 50) {
            return new ExperimentAssignmentDto("feed-ranking-v2", "control", RankingPolicy.CONTROL);
        }
        if (bucket < 70) {
            return new ExperimentAssignmentDto("feed-ranking-v2", "freshness_boost", RankingPolicy.FRESHNESS_BOOST);
        }
        if (bucket < 90) {
            return new ExperimentAssignmentDto("feed-ranking-v2", "diversity_boost", RankingPolicy.DIVERSITY_BOOST);
        }
        return new ExperimentAssignmentDto("feed-ranking-v2", "exploration_boost", RankingPolicy.EXPLORATION_BOOST);
    }
}
