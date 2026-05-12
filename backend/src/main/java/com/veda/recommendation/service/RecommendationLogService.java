package com.veda.recommendation.service;

import com.veda.recommendation.dto.RecommendationLogDto;
import com.veda.recommendation.entity.Content;
import com.veda.recommendation.entity.RecommendationLog;
import com.veda.recommendation.repository.ContentRepository;
import com.veda.recommendation.repository.RecommendationLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationLogService {
    private final RecommendationLogRepository recommendationLogRepository;
    private final ContentRepository contentRepository;

    public RecommendationLogService(
            RecommendationLogRepository recommendationLogRepository,
            ContentRepository contentRepository
    ) {
        this.recommendationLogRepository = recommendationLogRepository;
        this.contentRepository = contentRepository;
    }

    @Transactional(readOnly = true)
    public List<RecommendationLogDto> recentForUser(Long userId, int limit) {
        List<RecommendationLog> logs = recommendationLogRepository.findByUserIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(0, limit)
        );
        Map<Long, Content> contentById = contentRepository.findAllById(
                        logs.stream().map(RecommendationLog::getContentId).toList()
                )
                .stream()
                .collect(Collectors.toMap(Content::getId, Function.identity()));

        return logs.stream()
                .map(log -> toDto(log, contentById.get(log.getContentId())))
                .toList();
    }

    private RecommendationLogDto toDto(RecommendationLog log, Content content) {
        return new RecommendationLogDto(
                log.getId(),
                log.getUserId(),
                log.getContentId(),
                content == null ? "Unknown content" : content.getTitle(),
                log.getPredictedCtr(),
                log.getFinalScore(),
                log.getRankPosition(),
                log.getModelVersion(),
                log.getRankingPolicy(),
                log.getExperimentBucket(),
                log.getLatencyMs(),
                log.getCreatedAt()
        );
    }
}
