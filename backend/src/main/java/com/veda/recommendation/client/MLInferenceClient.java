package com.veda.recommendation.client;

import com.veda.recommendation.dto.RankingFeatureDto;
import com.veda.recommendation.dto.ModelMetadataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class MLInferenceClient {
    private static final Logger log = LoggerFactory.getLogger(MLInferenceClient.class);

    private final RestClient restClient;

    public MLInferenceClient(RestClient.Builder restClientBuilder, @Value("${recommendation.ml-service-url}") String mlServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(mlServiceUrl).build();
    }

    public PredictionBatch predict(List<RankingFeatureDto> features) {
        try {
            PredictionResponse response = restClient.post()
                    .uri("/predict")
                    .body(new PredictionRequest(features))
                    .retrieve()
                    .body(PredictionResponse.class);

            if (response == null || response.predictions() == null || response.predictions().size() != features.size()) {
                return fallback(features);
            }
            return new PredictionBatch(
                    response.predictions().stream().map(Prediction::predicted_ctr).toList(),
                    new ModelMetadataDto(response.model_version(), response.model_stage(), response.using_fallback(), response.loaded_at())
            );
        } catch (RestClientException exception) {
            log.warn("ML inference service unavailable, using deterministic fallback: {}", exception.getMessage());
            return fallback(features);
        }
    }

    public ModelMetadataDto metadata() {
        try {
            ModelMetadataDto metadata = restClient.get()
                    .uri("/metadata")
                    .retrieve()
                    .body(ModelMetadataDto.class);
            return metadata == null ? ModelMetadataDto.fallback() : metadata;
        } catch (RestClientException exception) {
            log.warn("ML metadata unavailable, using fallback metadata: {}", exception.getMessage());
            return ModelMetadataDto.fallback();
        }
    }

    private PredictionBatch fallback(List<RankingFeatureDto> features) {
        return new PredictionBatch(features.stream()
                .map(feature -> clamp(
                        0.28
                                + 0.22 * feature.categoryMatch()
                                + 0.18 * feature.contentEngagementRate()
                                + 0.14 * feature.contentPopularityScore()
                                + 0.10 * feature.contentFreshnessScore()
                                + 0.08 * feature.creatorAffinity()
                                - 0.12 * feature.userSkipRate()
                ))
                .toList(), ModelMetadataDto.fallback());
    }

    private double clamp(double value) {
        return Math.max(0.01, Math.min(0.99, value));
    }

    public record PredictionRequest(List<RankingFeatureDto> features) {
    }

    public record PredictionResponse(
            List<Prediction> predictions,
            String model_version,
            String model_stage,
            boolean using_fallback,
            String loaded_at
    ) {
    }

    public record Prediction(double predicted_ctr, double confidence) {
    }

    public record PredictionBatch(List<Double> predictedCtrs, ModelMetadataDto metadata) {
    }
}
