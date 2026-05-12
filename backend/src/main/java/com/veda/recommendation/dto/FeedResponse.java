package com.veda.recommendation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FeedResponse(
        Long userId,
        LocalDateTime generatedAt,
        long latencyMs,
        int candidateCount,
        String experimentName,
        String experimentBucket,
        String rankingPolicy,
        String modelVersion,
        List<RankedContentDto> items
) {
}
