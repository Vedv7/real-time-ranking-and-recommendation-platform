package com.veda.recommendation.dto;

import com.veda.recommendation.enums.ContentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateContentRequest(
        @NotNull Long creatorId,
        @NotBlank String title,
        @NotNull ContentCategory category,
        List<String> tags,
        Integer durationSeconds,
        String language,
        Double qualityScore
) {
}
