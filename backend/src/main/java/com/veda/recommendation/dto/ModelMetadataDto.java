package com.veda.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModelMetadataDto(
        @JsonProperty("model_version") String modelVersion,
        @JsonProperty("model_stage") String modelStage,
        @JsonProperty("using_fallback") boolean usingFallback,
        @JsonProperty("loaded_at") String loadedAt
) {
    public static ModelMetadataDto fallback() {
        return new ModelMetadataDto("deterministic-fallback-v1", "fallback", true, "runtime");
    }
}
