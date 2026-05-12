package com.veda.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RankingFeatureDto(
        @JsonProperty("user_total_interactions") long userTotalInteractions,
        @JsonProperty("user_avg_watch_time") double userAvgWatchTime,
        @JsonProperty("user_like_rate") double userLikeRate,
        @JsonProperty("user_skip_rate") double userSkipRate,
        @JsonProperty("content_popularity_score") double contentPopularityScore,
        @JsonProperty("content_freshness_score") double contentFreshnessScore,
        @JsonProperty("content_engagement_rate") double contentEngagementRate,
        @JsonProperty("category_match") int categoryMatch,
        @JsonProperty("creator_affinity") double creatorAffinity,
        @JsonProperty("content_age_hours") double contentAgeHours
) {
}
