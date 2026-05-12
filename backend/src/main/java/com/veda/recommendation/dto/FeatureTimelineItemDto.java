package com.veda.recommendation.dto;

import com.veda.recommendation.enums.ContentCategory;
import com.veda.recommendation.enums.InteractionType;

import java.time.LocalDateTime;
import java.util.Set;

public record FeatureTimelineItemDto(
        LocalDateTime timestamp,
        Long contentId,
        String contentTitle,
        ContentCategory category,
        InteractionType interactionType,
        long totalInteractions,
        double likeRate,
        double skipRate,
        double shareRate,
        double avgWatchTime,
        Set<ContentCategory> preferredCategories
) {
}
