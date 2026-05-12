package com.veda.recommendation.service;

import com.veda.recommendation.dto.DemoResetResponse;
import com.veda.recommendation.entity.Content;
import com.veda.recommendation.entity.ContentFeature;
import com.veda.recommendation.entity.InteractionEvent;
import com.veda.recommendation.entity.User;
import com.veda.recommendation.entity.UserFeature;
import com.veda.recommendation.enums.ContentCategory;
import com.veda.recommendation.enums.InteractionType;
import com.veda.recommendation.repository.ContentFeatureRepository;
import com.veda.recommendation.repository.ContentRepository;
import com.veda.recommendation.repository.InteractionEventRepository;
import com.veda.recommendation.repository.RecommendationLogRepository;
import com.veda.recommendation.repository.UserFeatureRepository;
import com.veda.recommendation.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class DemoDataService {
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final InteractionEventRepository interactionEventRepository;
    private final UserFeatureRepository userFeatureRepository;
    private final ContentFeatureRepository contentFeatureRepository;
    private final RecommendationLogRepository recommendationLogRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public DemoDataService(
            UserRepository userRepository,
            ContentRepository contentRepository,
            InteractionEventRepository interactionEventRepository,
            UserFeatureRepository userFeatureRepository,
            ContentFeatureRepository contentFeatureRepository,
            RecommendationLogRepository recommendationLogRepository,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.interactionEventRepository = interactionEventRepository;
        this.userFeatureRepository = userFeatureRepository;
        this.contentFeatureRepository = contentFeatureRepository;
        this.recommendationLogRepository = recommendationLogRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public DemoResetResponse reset() {
        recommendationLogRepository.deleteAllInBatch();
        interactionEventRepository.deleteAllInBatch();
        userFeatureRepository.deleteAllInBatch();
        contentFeatureRepository.deleteAllInBatch();
        contentRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        clearRedis();

        User demoUser = saveUser("rankstream_demo", "25-34", "US");
        User mlEngineer = saveUser("ml_engineer", "25-34", "US");
        User creator = saveUser("systems_creator", "25-34", "CA");

        List<Content> items = List.of(
                saveContent(creator.getId(), "Kafka event streams in real-time ranking", ContentCategory.TECHNOLOGY, List.of("kafka", "streaming", "events"), 72, 0.94, 4),
                saveContent(creator.getId(), "XGBoost CTR modeling for feed ranking", ContentCategory.EDUCATION, List.of("xgboost", "ctr", "ml"), 118, 0.91, 8),
                saveContent(creator.getId(), "Redis feature cache design for low latency APIs", ContentCategory.TECHNOLOGY, List.of("redis", "caching", "latency"), 85, 0.89, 12),
                saveContent(mlEngineer.getId(), "How recommendation logs power offline evaluation", ContentCategory.EDUCATION, List.of("evaluation", "ranking", "logs"), 96, 0.86, 18),
                saveContent(mlEngineer.getId(), "A/B testing freshness versus engagement", ContentCategory.NEWS, List.of("experiments", "metrics"), 64, 0.78, 6),
                saveContent(creator.getId(), "Building Spring Boot APIs for ML serving", ContentCategory.TECHNOLOGY, List.of("spring-boot", "java", "ml-serving"), 103, 0.9, 28),
                saveContent(mlEngineer.getId(), "GPU gaming setup tour", ContentCategory.GAMING, List.of("gaming", "hardware"), 54, 0.67, 30),
                saveContent(creator.getId(), "Healthy meal prep for busy engineers", ContentCategory.FOOD, List.of("food", "fitness"), 47, 0.62, 36),
                saveContent(mlEngineer.getId(), "Travel vlog: remote work in Lisbon", ContentCategory.TRAVEL, List.of("travel", "remote-work"), 81, 0.58, 48),
                saveContent(creator.getId(), "Finance basics: index funds explained", ContentCategory.FINANCE, List.of("finance", "investing"), 93, 0.7, 72),
                saveContent(mlEngineer.getId(), "Feature stores explained with online and offline parity", ContentCategory.TECHNOLOGY, List.of("feature-store", "mlops"), 110, 0.95, 2),
                saveContent(creator.getId(), "Model monitoring: latency, drift, and fallback scoring", ContentCategory.EDUCATION, List.of("monitoring", "mlops"), 88, 0.92, 5)
        );

        List<InteractionEvent> events = List.of(
                saveEvent(demoUser.getId(), items.get(0).getId(), InteractionType.LIKE, 61),
                saveEvent(demoUser.getId(), items.get(1).getId(), InteractionType.SAVE, 102),
                saveEvent(demoUser.getId(), items.get(2).getId(), InteractionType.SHARE, 77),
                saveEvent(demoUser.getId(), items.get(6).getId(), InteractionType.SKIP, 4),
                saveEvent(demoUser.getId(), items.get(7).getId(), InteractionType.SKIP, 3),
                saveEvent(demoUser.getId(), items.get(8).getId(), InteractionType.VIEW, 18)
        );

        UserFeature demoFeature = new UserFeature();
        demoFeature.setUserId(demoUser.getId());
        demoFeature.setPreferredCategories(Set.of(ContentCategory.TECHNOLOGY, ContentCategory.EDUCATION));
        demoFeature.setAvgWatchTime(44.2);
        demoFeature.setLikeRate(0.28);
        demoFeature.setSkipRate(0.18);
        demoFeature.setShareRate(0.12);
        demoFeature.setTotalInteractions((long) events.size());
        demoFeature.setLastActiveAt(LocalDateTime.now());
        userFeatureRepository.save(demoFeature);

        return new DemoResetResponse(
                demoUser.getId(),
                3,
                items.size(),
                events.size(),
                "Demo data reset with a technology/ML-biased user profile."
        );
    }

    private User saveUser(String username, String ageGroup, String country) {
        User user = new User();
        user.setUsername(username);
        user.setAgeGroup(ageGroup);
        user.setCountry(country);
        return userRepository.save(user);
    }

    private Content saveContent(
            Long creatorId,
            String title,
            ContentCategory category,
            List<String> tags,
            int durationSeconds,
            double qualityScore,
            int ageHours
    ) {
        Content content = new Content();
        content.setCreatorId(creatorId);
        content.setTitle(title);
        content.setCategory(category);
        content.setTags(tags);
        content.setDurationSeconds(durationSeconds);
        content.setLanguage("en");
        content.setQualityScore(qualityScore);
        content.setCreatedAt(LocalDateTime.now().minusHours(ageHours));
        Content saved = contentRepository.save(content);

        ContentFeature feature = new ContentFeature();
        feature.setContentId(saved.getId());
        feature.setTotalViews(120L + ageHours);
        feature.setTotalLikes(28L + Math.max(1, 20 - ageHours / 4));
        feature.setTotalSkips((long) Math.max(2, ageHours / 8));
        feature.setTotalShares(8L + Math.max(0, 12 - ageHours / 6));
        feature.setAvgWatchTime(durationSeconds * 0.64);
        feature.setPopularityScore(Math.min(1.0, 0.25 + qualityScore * 0.55));
        feature.setFreshnessScore(Math.exp(-ageHours / 72.0));
        feature.setEngagementRate(Math.min(1.0, (feature.getTotalLikes() + feature.getTotalShares()) / (double) Math.max(1, feature.getTotalViews())));
        contentFeatureRepository.save(feature);
        return saved;
    }

    private InteractionEvent saveEvent(Long userId, Long contentId, InteractionType type, double watchTimeSeconds) {
        InteractionEvent event = new InteractionEvent();
        event.setUserId(userId);
        event.setContentId(contentId);
        event.setInteractionType(type);
        event.setWatchTimeSeconds(watchTimeSeconds);
        event.setDeviceType("demo-seed");
        event.setSessionId("demo-reset");
        event.setTimestamp(LocalDateTime.now());
        return interactionEventRepository.save(event);
    }

    private void clearRedis() {
        try {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        } catch (RuntimeException ignored) {
            // Demo reset remains useful even when Redis is unavailable.
        }
    }
}
