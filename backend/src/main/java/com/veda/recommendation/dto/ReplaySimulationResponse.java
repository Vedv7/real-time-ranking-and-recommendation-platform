package com.veda.recommendation.dto;

import com.veda.recommendation.enums.ContentCategory;

public record ReplaySimulationResponse(
        Long userId,
        int eventsReplayed,
        ContentCategory focusCategory,
        long positiveEvents,
        long negativeEvents,
        String message
) {
}
