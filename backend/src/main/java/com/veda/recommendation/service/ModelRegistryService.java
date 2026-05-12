package com.veda.recommendation.service;

import com.veda.recommendation.client.MLInferenceClient;
import com.veda.recommendation.dto.ModelMetadataDto;
import com.veda.recommendation.dto.ModelRegistryEntryDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModelRegistryService {
    private final MLInferenceClient mlInferenceClient;

    public ModelRegistryService(MLInferenceClient mlInferenceClient) {
        this.mlInferenceClient = mlInferenceClient;
    }

    public List<ModelRegistryEntryDto> entries() {
        ModelMetadataDto active = mlInferenceClient.metadata();
        return List.of(
                new ModelRegistryEntryDto(
                        active.modelVersion(),
                        active.modelStage(),
                        true,
                        active.usingFallback(),
                        active.loadedAt(),
                        active.usingFallback()
                                ? "Deterministic runtime fallback is serving because no trained artifact is active."
                                : "Active model loaded by the FastAPI inference service."
                ),
                new ModelRegistryEntryDto(
                        "deterministic-fallback-v1",
                        "fallback",
                        active.usingFallback(),
                        true,
                        "runtime",
                        "Always available safety model for local development and ML service outages."
                )
        );
    }
}
