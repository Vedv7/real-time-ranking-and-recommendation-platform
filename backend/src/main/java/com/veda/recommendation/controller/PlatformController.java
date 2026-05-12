package com.veda.recommendation.controller;

import com.veda.recommendation.client.MLInferenceClient;
import com.veda.recommendation.dto.DemoResetResponse;
import com.veda.recommendation.dto.FeatureStoreStatusDto;
import com.veda.recommendation.dto.ModelMetadataDto;
import com.veda.recommendation.dto.RecommendationLogDto;
import com.veda.recommendation.service.DemoDataService;
import com.veda.recommendation.service.FeatureStoreDiagnosticsService;
import com.veda.recommendation.service.RecommendationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/platform")
public class PlatformController {
    private final FeatureStoreDiagnosticsService featureStoreDiagnosticsService;
    private final MLInferenceClient mlInferenceClient;
    private final DemoDataService demoDataService;
    private final RecommendationLogService recommendationLogService;

    public PlatformController(
            FeatureStoreDiagnosticsService featureStoreDiagnosticsService,
            MLInferenceClient mlInferenceClient,
            DemoDataService demoDataService,
            RecommendationLogService recommendationLogService
    ) {
        this.featureStoreDiagnosticsService = featureStoreDiagnosticsService;
        this.mlInferenceClient = mlInferenceClient;
        this.demoDataService = demoDataService;
        this.recommendationLogService = recommendationLogService;
    }

    @GetMapping("/feature-store")
    public FeatureStoreStatusDto featureStoreStatus() {
        return featureStoreDiagnosticsService.status();
    }

    @GetMapping("/model")
    public ModelMetadataDto modelMetadata() {
        return mlInferenceClient.metadata();
    }

    @GetMapping("/experiments")
    public Map<String, Object> experiments() {
        return Map.of(
                "activeExperiment", "feed-ranking-v2",
                "buckets", Map.of(
                        "control", "baseline CTR + freshness + popularity + category match",
                        "freshness_boost", "extra freshness weight for recent content",
                        "diversity_boost", "category repetition penalty",
                        "exploration_boost", "novel category exploration bonus"
                )
        );
    }

    @PostMapping("/demo/reset")
    public DemoResetResponse resetDemoData() {
        return demoDataService.reset();
    }

    @GetMapping("/recommendation-logs/{userId}")
    public List<RecommendationLogDto> recommendationLogs(@PathVariable Long userId) {
        return recommendationLogService.recentForUser(userId, 20);
    }
}
