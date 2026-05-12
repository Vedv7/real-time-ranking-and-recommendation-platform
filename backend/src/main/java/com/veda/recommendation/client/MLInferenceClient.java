package com.veda.recommendation.client;

import com.veda.recommendation.dto.RankingFeatureDto;
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

    public List<Double> predict(List<RankingFeatureDto> features) {
        try {
            PredictionResponse response = restClient.post()
                    .uri("/predict")
                    .body(new PredictionRequest(features))
                    .retrieve()
                    .body(PredictionResponse.class);

            if (response == null || response.predictions() == null || response.predictions().size() != features.size()) {
                return fallback(features);
            }
            return response.predictions().stream()
                    .map(Prediction::predicted_ctr)
                    .toList();
        } catch (RestClientException exception) {
            log.warn("ML inference service unavailable, using deterministic fallback: {}", exception.getMessage());
            return fallback(features);
        }
    }

    private List<Double> fallback(List<RankingFeatureDto> features) {
        return features.stream()
                .map(feature -> clamp(
                        0.28
                                + 0.22 * feature.categoryMatch()
                                + 0.18 * feature.contentEngagementRate()
                                + 0.14 * feature.contentPopularityScore()
                                + 0.10 * feature.contentFreshnessScore()
                                + 0.08 * feature.creatorAffinity()
                                - 0.12 * feature.userSkipRate()
                ))
                .toList();
    }

    private double clamp(double value) {
        return Math.max(0.01, Math.min(0.99, value));
    }

    public record PredictionRequest(List<RankingFeatureDto> features) {
    }

    public record PredictionResponse(List<Prediction> predictions) {
    }

    public record Prediction(double predicted_ctr, double confidence) {
    }
}
