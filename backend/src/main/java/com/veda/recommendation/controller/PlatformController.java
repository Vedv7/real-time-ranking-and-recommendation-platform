package com.veda.recommendation.controller;

import com.veda.recommendation.client.MLInferenceClient;
import com.veda.recommendation.dto.DemoResetResponse;
import com.veda.recommendation.dto.ExperimentResultDto;
import com.veda.recommendation.dto.FeatureStoreStatusDto;
import com.veda.recommendation.dto.FeatureTimelineItemDto;
import com.veda.recommendation.dto.ModelMetadataDto;
import com.veda.recommendation.dto.ModelRegistryEntryDto;
import com.veda.recommendation.dto.PlatformStatsDto;
import com.veda.recommendation.dto.RecommendationLogDto;
import com.veda.recommendation.dto.ReplaySimulationRequest;
import com.veda.recommendation.dto.ReplaySimulationResponse;
import com.veda.recommendation.service.DemoDataService;
import com.veda.recommendation.service.ExperimentAnalyticsService;
import com.veda.recommendation.service.FeatureTimelineService;
import com.veda.recommendation.service.FeatureStoreDiagnosticsService;
import com.veda.recommendation.service.ModelRegistryService;
import com.veda.recommendation.service.PlatformStatsService;
import com.veda.recommendation.service.RecommendationLogService;
import com.veda.recommendation.service.ReplaySimulationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/platform")
public class PlatformController {
    private final FeatureStoreDiagnosticsService featureStoreDiagnosticsService;
    private final MLInferenceClient mlInferenceClient;
    private final DemoDataService demoDataService;
    private final RecommendationLogService recommendationLogService;
    private final PlatformStatsService platformStatsService;
    private final ReplaySimulationService replaySimulationService;
    private final FeatureTimelineService featureTimelineService;
    private final ExperimentAnalyticsService experimentAnalyticsService;
    private final ModelRegistryService modelRegistryService;
    private final boolean demoResetEnabled;

    public PlatformController(
            FeatureStoreDiagnosticsService featureStoreDiagnosticsService,
            MLInferenceClient mlInferenceClient,
            DemoDataService demoDataService,
            RecommendationLogService recommendationLogService,
            PlatformStatsService platformStatsService,
            ReplaySimulationService replaySimulationService,
            FeatureTimelineService featureTimelineService,
            ExperimentAnalyticsService experimentAnalyticsService,
            ModelRegistryService modelRegistryService,
            @Value("${recommendation.demo-reset-enabled:true}") boolean demoResetEnabled
    ) {
        this.featureStoreDiagnosticsService = featureStoreDiagnosticsService;
        this.mlInferenceClient = mlInferenceClient;
        this.demoDataService = demoDataService;
        this.recommendationLogService = recommendationLogService;
        this.platformStatsService = platformStatsService;
        this.replaySimulationService = replaySimulationService;
        this.featureTimelineService = featureTimelineService;
        this.experimentAnalyticsService = experimentAnalyticsService;
        this.modelRegistryService = modelRegistryService;
        this.demoResetEnabled = demoResetEnabled;
    }

    @GetMapping("/feature-store")
    public FeatureStoreStatusDto featureStoreStatus() {
        return featureStoreDiagnosticsService.status();
    }

    @GetMapping("/model")
    public ModelMetadataDto modelMetadata() {
        return mlInferenceClient.metadata();
    }

    @GetMapping("/model-registry")
    public List<ModelRegistryEntryDto> modelRegistry() {
        return modelRegistryService.entries();
    }

    @GetMapping("/stats")
    public PlatformStatsDto stats() {
        return platformStatsService.snapshot();
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

    @GetMapping("/experiments/results")
    public List<ExperimentResultDto> experimentResults() {
        return experimentAnalyticsService.results();
    }

    @PostMapping("/demo/reset")
    public DemoResetResponse resetDemoData() {
        if (!demoResetEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Demo reset is disabled");
        }
        return demoDataService.reset();
    }

    @PostMapping("/replay")
    public ReplaySimulationResponse replay(@RequestBody ReplaySimulationRequest request) {
        return replaySimulationService.replay(request);
    }

    @GetMapping("/recommendation-logs/{userId}")
    public List<RecommendationLogDto> recommendationLogs(@PathVariable Long userId) {
        return recommendationLogService.recentForUser(userId, 20);
    }

    @GetMapping("/feature-timeline/{userId}")
    public List<FeatureTimelineItemDto> featureTimeline(@PathVariable Long userId) {
        return featureTimelineService.timeline(userId);
    }
}
