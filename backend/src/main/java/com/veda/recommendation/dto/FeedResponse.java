package com.veda.recommendation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FeedResponse(
        Long userId,
        LocalDateTime generatedAt,
        long latencyMs,
        List<RankedContentDto> items
) {
}
