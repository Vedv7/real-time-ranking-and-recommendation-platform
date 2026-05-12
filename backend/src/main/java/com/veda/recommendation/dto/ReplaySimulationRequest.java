package com.veda.recommendation.dto;

import com.veda.recommendation.enums.ContentCategory;

public record ReplaySimulationRequest(
        Long userId,
        Integer eventCount,
        ContentCategory focusCategory
) {
}
