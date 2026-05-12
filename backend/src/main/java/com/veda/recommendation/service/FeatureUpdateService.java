package com.veda.recommendation.service;

import com.veda.recommendation.dto.InteractionEventRequest;
import com.veda.recommendation.entity.Content;
import com.veda.recommendation.entity.ContentFeature;
import com.veda.recommendation.entity.InteractionEvent;
import com.veda.recommendation.entity.UserFeature;
import com.veda.recommendation.enums.InteractionType;
import com.veda.recommendation.repository.ContentFeatureRepository;
import com.veda.recommendation.repository.ContentRepository;
import com.veda.recommendation.repository.InteractionEventRepository;
import com.veda.recommendation.repository.UserFeatureRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class FeatureUpdateService {
    private final UserFeatureRepository userFeatureRepository;
    private final ContentFeatureRepository contentFeatureRepository;
    private final ContentRepository contentRepository;
    private final InteractionEventRepository interactionEventRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public FeatureUpdateService(
            UserFeatureRepository userFeatureRepository,
            ContentFeatureRepository contentFeatureRepository,
            ContentRepository contentRepository,
            InteractionEventRepository interactionEventRepository,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.userFeatureRepository = userFeatureRepository;
        this.contentFeatureRepository = contentFeatureRepository;
        this.contentRepository = contentRepository;
        this.interactionEventRepository = interactionEventRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void updateFeatures(InteractionEventRequest event) {
        Content content = contentRepository.findById(event.contentId()).orElse(null);
        UserFeature userFeature = userFeatureRepository.findById(event.userId()).orElseGet(() -> {
            UserFeature feature = new UserFeature();
            feature.setUserId(event.userId());
            return feature;
        });
        ContentFeature contentFeature = contentFeatureRepository.findById(event.contentId()).orElseGet(() -> {
            ContentFeature feature = new ContentFeature();
            feature.setContentId(event.contentId());
            return feature;
        });

        updateUserFeature(userFeature, event, content);
        updateContentFeature(contentFeature, event, content);

        userFeatureRepository.save(userFeature);
        contentFeatureRepository.save(contentFeature);
        cache("user-feature:" + event.userId(), userFeature);
        cache("content-feature:" + event.contentId(), contentFeature);
        deleteFeedCaches(event.userId());
    }

    private void updateUserFeature(UserFeature feature, InteractionEventRequest event, Content content) {
        List<InteractionEvent> events = interactionEventRepository.findByUserIdOrderByTimestampDesc(event.userId());
        long total = Math.max(1, events.size());
        long likes = events.stream().filter(e -> e.getInteractionType() == InteractionType.LIKE).count();
        long skips = events.stream().filter(e -> e.getInteractionType() == InteractionType.SKIP).count();
        long shares = events.stream().filter(e -> e.getInteractionType() == InteractionType.SHARE).count();
        double avgWatch = events.stream()
                .map(InteractionEvent::getWatchTimeSeconds)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        feature.setTotalInteractions(total);
        feature.setLikeRate(likes / (double) total);
        feature.setSkipRate(skips / (double) total);
        feature.setShareRate(shares / (double) total);
        feature.setAvgWatchTime(avgWatch);
        feature.setLastActiveAt(event.timestamp() == null ? LocalDateTime.now() : event.timestamp());

        if (content != null && isPositive(event.interactionType())) {
            feature.getPreferredCategories().add(content.getCategory());
        }
    }

    private void updateContentFeature(ContentFeature feature, InteractionEventRequest event, Content content) {
        if (event.interactionType() == InteractionType.VIEW) {
            feature.setTotalViews(feature.getTotalViews() + 1);
        } else if (event.interactionType() == InteractionType.LIKE || event.interactionType() == InteractionType.CLICK) {
            feature.setTotalLikes(feature.getTotalLikes() + 1);
        } else if (event.interactionType() == InteractionType.SKIP) {
            feature.setTotalSkips(feature.getTotalSkips() + 1);
        } else if (event.interactionType() == InteractionType.SHARE) {
            feature.setTotalShares(feature.getTotalShares() + 1);
        }

        if (event.watchTimeSeconds() != null && event.watchTimeSeconds() > 0) {
            long views = Math.max(1, feature.getTotalViews());
            feature.setAvgWatchTime(((feature.getAvgWatchTime() * Math.max(0, views - 1)) + event.watchTimeSeconds()) / views);
        }

        long impressions = Math.max(1, feature.getTotalViews() + feature.getTotalSkips());
        double engagement = (feature.getTotalLikes() + feature.getTotalShares()) / (double) impressions;
        double popularity = Math.min(1.0, Math.log1p(feature.getTotalViews() + 3.0 * feature.getTotalLikes() + 5.0 * feature.getTotalShares()) / 10.0);
        double freshness = content == null ? 0.5 : freshness(content.getCreatedAt());

        feature.setEngagementRate(engagement);
        feature.setPopularityScore(popularity);
        feature.setFreshnessScore(freshness);
    }

    private boolean isPositive(InteractionType type) {
        return type == InteractionType.LIKE || type == InteractionType.CLICK || type == InteractionType.SAVE || type == InteractionType.SHARE;
    }

    private double freshness(LocalDateTime createdAt) {
        long hours = Math.max(0, Duration.between(createdAt, LocalDateTime.now()).toHours());
        return Math.exp(-hours / 72.0);
    }

    private void cache(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofHours(2));
        } catch (RuntimeException ignored) {
            // Redis should not block durable feature updates in local development.
        }
    }

    private void deleteFeedCaches(Long userId) {
        try {
            Set<String> keys = redisTemplate.keys("feed:" + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException ignored) {
            // Cache invalidation is best effort; the next TTL expiry will refresh the feed.
        }
    }
}
