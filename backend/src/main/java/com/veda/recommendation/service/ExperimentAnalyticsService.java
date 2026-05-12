package com.veda.recommendation.service;

import com.veda.recommendation.dto.ExperimentResultDto;
import com.veda.recommendation.entity.RecommendationLog;
import com.veda.recommendation.repository.RecommendationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExperimentAnalyticsService {
    private final RecommendationLogRepository recommendationLogRepository;

    public ExperimentAnalyticsService(RecommendationLogRepository recommendationLogRepository) {
        this.recommendationLogRepository = recommendationLogRepository;
    }

    @Transactional(readOnly = true)
    public List<ExperimentResultDto> results() {
        return recommendationLogRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(log -> log.getExperimentBucket() + "|" + log.getRankingPolicy()))
                .entrySet()
                .stream()
                .map(entry -> toResult(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(ExperimentResultDto::impressions).reversed())
                .toList();
    }

    private ExperimentResultDto toResult(String key, List<RecommendationLog> logs) {
        String[] parts = key.split("\\|", 2);
        long impressions = logs.size();
        long topRanked = logs.stream().filter(log -> log.getRankPosition() != null && log.getRankPosition() == 1).count();
        return new ExperimentResultDto(
                parts[0],
                parts.length > 1 ? parts[1] : "unknown",
                impressions,
                round(avg(logs, RecommendationLog::getPredictedCtr)),
                round(avg(logs, RecommendationLog::getFinalScore)),
                round(avg(logs, log -> log.getLatencyMs() == null ? null : log.getLatencyMs().doubleValue())),
                round(topRanked / (double) Math.max(1, impressions))
        );
    }

    private double avg(List<RecommendationLog> logs, Metric metric) {
        return logs.stream()
                .map(metric::value)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    @FunctionalInterface
    private interface Metric {
        Double value(RecommendationLog log);
    }
}
