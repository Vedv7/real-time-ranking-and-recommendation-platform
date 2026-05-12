package com.veda.recommendation.service;

import com.veda.recommendation.dto.FeatureTimelineItemDto;
import com.veda.recommendation.entity.Content;
import com.veda.recommendation.entity.InteractionEvent;
import com.veda.recommendation.enums.ContentCategory;
import com.veda.recommendation.enums.InteractionType;
import com.veda.recommendation.repository.ContentRepository;
import com.veda.recommendation.repository.InteractionEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FeatureTimelineService {
    private final InteractionEventRepository interactionEventRepository;
    private final ContentRepository contentRepository;

    public FeatureTimelineService(
            InteractionEventRepository interactionEventRepository,
            ContentRepository contentRepository
    ) {
        this.interactionEventRepository = interactionEventRepository;
        this.contentRepository = contentRepository;
    }

    @Transactional(readOnly = true)
    public List<FeatureTimelineItemDto> timeline(Long userId) {
        List<InteractionEvent> events = interactionEventRepository.findByUserIdOrderByTimestampAsc(userId);
        Map<Long, Content> contentById = contentRepository.findAllById(
                        events.stream().map(InteractionEvent::getContentId).toList()
                )
                .stream()
                .collect(Collectors.toMap(Content::getId, Function.identity()));

        long likes = 0;
        long skips = 0;
        long shares = 0;
        double watchSum = 0.0;
        long watchCount = 0;
        Set<ContentCategory> preferredCategories = new HashSet<>();
        List<FeatureTimelineItemDto> timeline = new ArrayList<>();

        for (int index = 0; index < events.size(); index++) {
            InteractionEvent event = events.get(index);
            Content content = contentById.get(event.getContentId());
            if (event.getInteractionType() == InteractionType.LIKE) {
                likes++;
            }
            if (event.getInteractionType() == InteractionType.SKIP) {
                skips++;
            }
            if (event.getInteractionType() == InteractionType.SHARE) {
                shares++;
            }
            if (event.getWatchTimeSeconds() != null) {
                watchSum += event.getWatchTimeSeconds();
                watchCount++;
            }
            if (content != null && isPositive(event.getInteractionType())) {
                preferredCategories.add(content.getCategory());
            }

            long total = index + 1L;
            timeline.add(new FeatureTimelineItemDto(
                    event.getTimestamp(),
                    event.getContentId(),
                    content == null ? "Unknown content" : content.getTitle(),
                    content == null ? null : content.getCategory(),
                    event.getInteractionType(),
                    total,
                    round(likes / (double) total),
                    round(skips / (double) total),
                    round(shares / (double) total),
                    round(watchCount == 0 ? 0.0 : watchSum / watchCount),
                    Set.copyOf(preferredCategories)
            ));
        }

        return timeline.stream()
                .skip(Math.max(0, timeline.size() - 20))
                .toList();
    }

    private boolean isPositive(InteractionType type) {
        return type == InteractionType.LIKE || type == InteractionType.CLICK || type == InteractionType.SAVE || type == InteractionType.SHARE;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
