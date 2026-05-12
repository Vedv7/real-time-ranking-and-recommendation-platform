package com.veda.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veda.recommendation.client.MLInferenceClient;
import com.veda.recommendation.dto.RankedContentDto;
import com.veda.recommendation.dto.RankingFeatureDto;
import com.veda.recommendation.entity.Content;
import com.veda.recommendation.entity.ContentFeature;
import com.veda.recommendation.entity.InteractionEvent;
import com.veda.recommendation.entity.RecommendationLog;
import com.veda.recommendation.entity.UserFeature;
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
    private final ObjectMapper objectMapper;

    public RankingService(
            UserFeatureRepository userFeatureRepository,
            ContentFeatureRepository contentFeatureRepository,
            InteractionEventRepository interactionEventRepository,
            ContentRepository contentRepository,
            RecommendationLogRepository recommendationLogRepository,
            MLInferenceClient mlInferenceClient,
            ObjectMapper objectMapper
    ) {
        this.userFeatureRepository = userFeatureRepository;
        this.contentFeatureRepository = contentFeatureRepository;
        this.interactionEventRepository = interactionEventRepository;
        this.contentRepository = contentRepository;
        this.recommendationLogRepository = recommendationLogRepository;
        this.mlInferenceClient = mlInferenceClient;
        this.objectMapper = objectMapper;
    }

    public List<RankedContentDto> rank(Long userId, List<Content> candidates, int limit, long requestLatencyMs) {
        long started = System.nanoTime();
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
        List<Double> predictedCtrs = mlInferenceClient.predict(featureVectors);
        long rankingLatencyMs = Duration.ofNanos(System.nanoTime() - started).toMillis();

        List<ScoredCandidate> scored = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            RankingFeatureDto features = featureVectors.get(i);
            double predictedCtr = predictedCtrs.get(i);
            double finalScore = 0.65 * predictedCtr
                    + 0.15 * features.contentFreshnessScore()
                    + 0.10 * features.contentPopularityScore()
                    + 0.10 * features.categoryMatch();
            scored.add(new ScoredCandidate(candidates.get(i), features, predictedCtr, finalScore));
        }

        List<ScoredCandidate> top = scored.stream()
                .sorted(Comparator.comparingDouble(ScoredCandidate::finalScore).reversed())
                .limit(limit)
                .toList();

        List<RankedContentDto> ranked = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            ScoredCandidate item = top.get(i);
            int rank = i + 1;
            ranked.add(toDto(item, rank));
            persistLog(userId, item, rank, Math.max(requestLatencyMs, rankingLatencyMs));
        }
        return ranked;
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

    private RankedContentDto toDto(ScoredCandidate item, int rankPosition) {
        RankingFeatureDto features = item.features();
        return new RankedContentDto(
                item.content().getId(),
                item.content().getTitle(),
                item.content().getCategory(),
                round(item.predictedCtr()),
                round(features.contentPopularityScore()),
                round(features.categoryMatch()),
                round(features.contentFreshnessScore()),
                round(item.finalScore()),
                rankPosition,
                explanation(features, item.predictedCtr())
        );
    }

    private List<String> explanation(RankingFeatureDto features, double predictedCtr) {
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
        if (reasons.isEmpty()) {
            reasons.add("Balanced exploration candidate");
        }
        return reasons;
    }

    private void persistLog(Long userId, ScoredCandidate item, int rankPosition, long latencyMs) {
        RecommendationLog log = new RecommendationLog();
        log.setUserId(userId);
        log.setContentId(item.content().getId());
        log.setPredictedCtr(item.predictedCtr());
        log.setFinalScore(item.finalScore());
        log.setRankPosition(rankPosition);
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

    private record ScoredCandidate(Content content, RankingFeatureDto features, double predictedCtr, double finalScore) {
    }
}
