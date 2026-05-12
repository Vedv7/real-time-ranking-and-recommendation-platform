package com.veda.recommendation.service;

import com.veda.recommendation.dto.FeedResponse;
import com.veda.recommendation.dto.RankedContentDto;
import com.veda.recommendation.entity.Content;
import com.veda.recommendation.exception.ResourceNotFoundException;
import com.veda.recommendation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeedService {
    private final UserRepository userRepository;
    private final CandidateGenerationService candidateGenerationService;
    private final RankingService rankingService;
    private final MetricsService metricsService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final long feedCacheTtlSeconds;

    public FeedService(
            UserRepository userRepository,
            CandidateGenerationService candidateGenerationService,
            RankingService rankingService,
            MetricsService metricsService,
            RedisTemplate<String, Object> redisTemplate,
            @Value("${recommendation.feed-cache-ttl-seconds}") long feedCacheTtlSeconds
    ) {
        this.userRepository = userRepository;
        this.candidateGenerationService = candidateGenerationService;
        this.rankingService = rankingService;
        this.metricsService = metricsService;
        this.redisTemplate = redisTemplate;
        this.feedCacheTtlSeconds = feedCacheTtlSeconds;
    }

    public FeedResponse getFeed(Long userId, int limit) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        String cacheKey = "feed:" + userId + ":" + limit;
        FeedResponse cached = cached(cacheKey);
        if (cached != null) {
            return cached;
        }

        long started = System.nanoTime();
        List<Content> candidates = candidateGenerationService.generateCandidates(userId);
        List<RankedContentDto> items = rankingService.rank(userId, candidates, limit, 0);
        long latencyMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
        FeedResponse response = new FeedResponse(userId, LocalDateTime.now(), latencyMs, items);

        cache(cacheKey, response);
        metricsService.recordInferenceLatency(userId, candidates.size(), latencyMs);
        metricsService.recordFeedRequest(userId, limit, items.size(), latencyMs);
        return response;
    }

    private FeedResponse cached(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value instanceof FeedResponse response ? response : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void cache(String key, FeedResponse value) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(feedCacheTtlSeconds));
        } catch (RuntimeException ignored) {
            // Feed generation remains available when Redis is not running.
        }
    }
}
