package com.veda.recommendation.dto;

public record ModelRegistryEntryDto(
        String modelVersion,
        String stage,
        boolean active,
        boolean fallback,
        String loadedAt,
        String notes
) {
}
