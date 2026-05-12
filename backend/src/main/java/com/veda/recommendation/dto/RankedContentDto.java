package com.veda.recommendation.dto;

import com.veda.recommendation.enums.ContentCategory;

import java.util.List;

public record RankedContentDto(
        Long contentId,
        String title,
        ContentCategory category,
        double predictedCtr,
        double contentPopularityScore,
        double userInterestMatchScore,
        double freshnessScore,
        double finalScore,
        int rankPosition,
        List<String> explanation
) {
}
