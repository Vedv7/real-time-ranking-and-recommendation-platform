package com.veda.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veda.recommendation.client.MLInferenceClient;
import com.veda.recommendation.dto.ExperimentAssignmentDto;
import com.veda.recommendation.dto.ModelMetadataDto;
import com.veda.recommendation.dto.RankedContentDto;
import com.veda.recommendation.dto.RankingFeatureDto;
import com.veda.recommendation.entity.Content;
import com.veda.recommendation.entity.ContentFeature;
import com.veda.recommendation.entity.InteractionEvent;
import com.veda.recommendation.entity.RecommendationLog;
import com.veda.recommendation.entity.UserFeature;
import com.veda.recommendation.enums.ContentCategory;
import com.veda.recommendation.enums.RankingPolicy;
import com.veda.recommendation.repository.ContentFeatureRepository;
import com.veda.recommendation.repository.ContentRepository;
import com.veda.recommendation.repository.InteractionEventRepository;
import com.veda.recommendation.repository.RecommendationLogRepository;
import com.veda.recommendation.repository.UserFeatureRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RankingService {
    private final UserFeatureRepository userFeatureRepository;
    private final ContentFeatureRepository contentFeatureRepository;
    private final InteractionEventRepository interactionEventRepository;
    private final ContentRepository contentRepository;
    private final RecommendationLogRepository recommendationLogRepository;
    private final MLInferenceClient mlInferenceClient;
    private final ExperimentAssignmentService experimentAssignmentService;
    private final ObjectMapper objectMapper;

    public RankingService(
            UserFeatureRepository userFeatureRepository,
            ContentFeatureRepository contentFeatureRepository,
            InteractionEventRepository interactionEventRepository,
            ContentRepository contentRepository,
            RecommendationLogRepository recommendationLogRepository,
            MLInferenceClient mlInferenceClient,
            ExperimentAssignmentService experimentAssignmentService,
            ObjectMapper objectMapper
    ) {
        this.userFeatureRepository = userFeatureRepository;
        this.contentFeatureRepository = contentFeatureRepository;
        this.interactionEventRepository = interactionEventRepository;
        this.contentRepository = contentRepository;
        this.recommendationLogRepository = recommendationLogRepository;
        this.mlInferenceClient = mlInferenceClient;
        this.experimentAssignmentService = experimentAssignmentService;
        this.objectMapper = objectMapper;
    }

    public RankingResult rank(Long userId, List<Content> candidates, int limit, long requestLatencyMs) {
        long started = System.nanoTime();
        ExperimentAssignmentDto assignment = experimentAssignmentService.assign(userId);
        UserFeature userFeature = userFeatureRepository.findById(userId).orElseGet(() -> {
            UserFeature feature = new UserFeature();
            feature.setUserId(userId);
            return feature;
        });

        Map<Long, ContentFeature> contentFeatures = contentFeatureRepository.findAllById(
                candidates.stream().map(Content::getId).toList()
        ).stream().collect(Collectors.toMap(ContentFeature::getContentId, Function.identity()));

        List<RankingFeatureDto> featureVectors = candidates.stream()
                .map(content -> buildFeatures(userId, userFeature, content, contentFeatures.get(content.getId())))
                .toList();
        MLInferenceClient.PredictionBatch predictionBatch = mlInferenceClient.predict(featureVectors);
        List<Double> predictedCtrs = predictionBatch.predictedCtrs();
        ModelMetadataDto modelMetadata = predictionBatch.metadata();
        long rankingLatencyMs = Duration.ofNanos(System.nanoTime() - started).toMillis();

        List<ScoredCandidate> scored = new ArrayList<>();
        Map<ContentCategory, Long> categoryCounts = candidates.stream()
                .collect(Collectors.groupingBy(Content::getCategory, Collectors.counting()));
        for (int i = 0; i < candidates.size(); i++) {
            Content content = candidates.get(i);
            RankingFeatureDto features = featureVectors.get(i);
            double predictedCtr = predictedCtrs.get(i);
            double explorationScore = explorationScore(content, userFeature, features);
            double diversityPenalty = diversityPenalty(content, categoryCounts);
            double finalScore = score(assignment.rankingPolicy(), predictedCtr, features, explorationScore, diversityPenalty);
            scored.add(new ScoredCandidate(content, features, predictedCtr, explorationScore, diversityPenalty, finalScore));
        }

        List<ScoredCandidate> top = scored.stream()
                .sorted(Comparator.comparingDouble(ScoredCandidate::finalScore).reversed())
                .limit(limit)
                .toList();

        List<RankedContentDto> ranked = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            ScoredCandidate item = top.get(i);
            int rank = i + 1;
            ranked.add(toDto(item, rank, assignment.rankingPolicy(), modelMetadata.modelVersion()));
            persistLog(userId, item, rank, Math.max(requestLatencyMs, rankingLatencyMs), assignment, modelMetadata);
        }
        return new RankingResult(ranked, assignment, modelMetadata, rankingLatencyMs);
    }

    private double score(
            RankingPolicy policy,
            double predictedCtr,
            RankingFeatureDto features,
            double explorationScore,
            double diversityPenalty
    ) {
        double base = 0.65 * predictedCtr
                + 0.15 * features.contentFreshnessScore()
                + 0.10 * features.contentPopularityScore()
                + 0.10 * features.categoryMatch();
        return switch (policy) {
            case FRESHNESS_BOOST -> base + 0.10 * features.contentFreshnessScore();
            case DIVERSITY_BOOST -> base - diversityPenalty;
            case EXPLORATION_BOOST -> base + 0.08 * explorationScore;
            case CONTROL -> base;
        };
    }

    private double explorationScore(Content content, UserFeature userFeature, RankingFeatureDto features) {
        double novelty = userFeature.getPreferredCategories().contains(content.getCategory()) ? 0.0 : 1.0;
        return Math.min(1.0, 0.6 * novelty + 0.4 * features.contentFreshnessScore());
    }

    private double diversityPenalty(Content content, Map<ContentCategory, Long> categoryCounts) {
        long categoryCount = categoryCounts.getOrDefault(content.getCategory(), 0L);
        return Math.min(0.15, Math.max(0, categoryCount - 10) * 0.01);
    }

    private RankingFeatureDto buildFeatures(Long userId, UserFeature userFeature, Content content, ContentFeature contentFeature) {
        ContentFeature safeContentFeature = contentFeature == null ? new ContentFeature() : contentFeature;
        if (safeContentFeature.getContentId() == null) {
            safeContentFeature.setContentId(content.getId());
        }
        double ageHours = Math.max(0, Duration.between(content.getCreatedAt(), LocalDateTime.now()).toHours());
        double freshness = contentFeature == null ? Math.exp(-ageHours / 72.0) : safeContentFeature.getFreshnessScore();
        int categoryMatch = userFeature.getPreferredCategories().contains(content.getCategory()) ? 1 : 0;

        return new RankingFeatureDto(
                userFeature.getTotalInteractions(),
                userFeature.getAvgWatchTime(),
                userFeature.getLikeRate(),
                userFeature.getSkipRate(),
                safeContentFeature.getPopularityScore(),
                freshness,
                safeContentFeature.getEngagementRate(),
                categoryMatch,
                creatorAffinity(userId, content.getCreatorId()),
                ageHours
        );
    }

    private double creatorAffinity(Long userId, Long creatorId) {
        if (creatorId == null) {
            return 0.0;
        }
        List<InteractionEvent> events = interactionEventRepository.findByUserIdOrderByTimestampDesc(userId);
        if (events.isEmpty()) {
            return 0.0;
        }
        Map<Long, Content> interactedContent = contentRepository.findAllById(
                events.stream().map(InteractionEvent::getContentId).toList()
        ).stream().collect(Collectors.toMap(Content::getId, Function.identity()));

        long sameCreator = events.stream()
                .map(event -> interactedContent.get(event.getContentId()))
                .filter(content -> content != null && creatorId.equals(content.getCreatorId()))
                .count();
        return Math.min(1.0, sameCreator / 10.0);
    }

    private RankedContentDto toDto(ScoredCandidate item, int rankPosition, RankingPolicy policy, String modelVersion) {
        RankingFeatureDto features = item.features();
        return new RankedContentDto(
                item.content().getId(),
                item.content().getTitle(),
                item.content().getCategory(),
                round(item.predictedCtr()),
                round(features.contentPopularityScore()),
                round(features.categoryMatch()),
                round(features.contentFreshnessScore()),
                round(item.explorationScore()),
                round(item.diversityPenalty()),
                round(item.finalScore()),
                rankPosition,
                policy.name(),
                modelVersion,
                explanation(features, item.predictedCtr(), item.explorationScore(), item.diversityPenalty())
        );
    }

    private List<String> explanation(RankingFeatureDto features, double predictedCtr, double explorationScore, double diversityPenalty) {
        List<String> reasons = new ArrayList<>();
        if (features.categoryMatch() == 1) {
            reasons.add("High match with user interests");
        }
        if (features.contentEngagementRate() > 0.25 || features.contentPopularityScore() > 0.4) {
            reasons.add("Strong engagement from similar users");
        }
        if (features.contentFreshnessScore() > 0.55) {
            reasons.add("Fresh content boost");
        }
        if (features.userSkipRate() < 0.2 && predictedCtr > 0.45) {
            reasons.add("Low skip probability");
        }
        if (explorationScore > 0.6) {
            reasons.add("Exploration candidate to learn new interests");
        }
        if (diversityPenalty > 0.0) {
            reasons.add("Diversity re-ranker reduced duplicate category exposure");
        }
        if (reasons.isEmpty()) {
            reasons.add("Balanced exploration candidate");
        }
        return reasons;
    }

    private void persistLog(
            Long userId,
            ScoredCandidate item,
            int rankPosition,
            long latencyMs,
            ExperimentAssignmentDto assignment,
            ModelMetadataDto modelMetadata
    ) {
        RecommendationLog log = new RecommendationLog();
        log.setUserId(userId);
        log.setContentId(item.content().getId());
        log.setPredictedCtr(item.predictedCtr());
        log.setFinalScore(item.finalScore());
        log.setRankPosition(rankPosition);
        log.setModelVersion(modelMetadata.modelVersion());
        log.setRankingPolicy(assignment.rankingPolicy().name());
        log.setExperimentBucket(assignment.bucket());
        log.setFeatureSnapshotJson(featureSnapshot(item.features()));
        log.setLatencyMs(latencyMs);
        recommendationLogRepository.save(log);
    }

    private String featureSnapshot(RankingFeatureDto features) {
        try {
            return objectMapper.writeValueAsString(features);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private record ScoredCandidate(
            Content content,
            RankingFeatureDto features,
            double predictedCtr,
            double explorationScore,
            double diversityPenalty,
            double finalScore
    ) {
    }

    public record RankingResult(
            List<RankedContentDto> items,
            ExperimentAssignmentDto assignment,
            ModelMetadataDto modelMetadata,
            long rankingLatencyMs
    ) {
    }
}
