package com.veda.recommendation.dto;

import com.veda.recommendation.enums.InteractionType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record InteractionEventRequest(
        @NotNull Long userId,
        @NotNull Long contentId,
        @NotNull InteractionType interactionType,
        Double watchTimeSeconds,
        LocalDateTime timestamp,
        String deviceType,
        String sessionId
) {
}
