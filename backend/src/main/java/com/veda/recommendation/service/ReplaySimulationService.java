package com.veda.recommendation.service;

import com.veda.recommendation.dto.InteractionEventRequest;
import com.veda.recommendation.dto.ReplaySimulationRequest;
import com.veda.recommendation.dto.ReplaySimulationResponse;
import com.veda.recommendation.entity.Content;
import com.veda.recommendation.entity.InteractionEvent;
import com.veda.recommendation.enums.ContentCategory;
import com.veda.recommendation.enums.InteractionType;
import com.veda.recommendation.exception.ResourceNotFoundException;
import com.veda.recommendation.repository.ContentRepository;
import com.veda.recommendation.repository.InteractionEventRepository;
import com.veda.recommendation.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ReplaySimulationService {
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final InteractionEventRepository interactionEventRepository;
    private final FeatureUpdateService featureUpdateService;

    public ReplaySimulationService(
            UserRepository userRepository,
            ContentRepository contentRepository,
            InteractionEventRepository interactionEventRepository,
            FeatureUpdateService featureUpdateService
    ) {
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.interactionEventRepository = interactionEventRepository;
        this.featureUpdateService = featureUpdateService;
    }

    @Transactional
    public ReplaySimulationResponse replay(ReplaySimulationRequest request) {
        Long userId = request.userId();
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        List<Content> content = contentRepository.findAll(PageRequest.of(0, 100)).getContent();
        if (content.isEmpty()) {
            throw new ResourceNotFoundException("No content available for replay");
        }

        ContentCategory focusCategory = request.focusCategory() == null
                ? content.get(0).getCategory()
                : request.focusCategory();
        List<Content> ordered = content.stream()
                .sorted(Comparator.comparing((Content item) -> item.getCategory() == focusCategory).reversed()
                        .thenComparing(Content::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int eventCount = Math.max(1, Math.min(request.eventCount() == null ? 25 : request.eventCount(), 500));
        long positiveEvents = 0;
        long negativeEvents = 0;
        for (int i = 0; i < eventCount; i++) {
            Content item = ordered.get(i % ordered.size());
            boolean focused = item.getCategory() == focusCategory;
            InteractionType type = interactionType(i, focused);
            if (type == InteractionType.SKIP) {
                negativeEvents++;
            } else {
                positiveEvents++;
            }
            InteractionEvent saved = saveEvent(userId, item, type, i, eventCount);
            featureUpdateService.updateFeatures(toRequest(saved));
        }

        return new ReplaySimulationResponse(
                userId,
                eventCount,
                focusCategory,
                positiveEvents,
                negativeEvents,
                "Replay complete. User features and feed cache were updated synchronously."
        );
    }

    private InteractionType interactionType(int index, boolean focused) {
        if (!focused && index % 2 == 0) {
            return InteractionType.SKIP;
        }
        return switch (index % 5) {
            case 0 -> InteractionType.LIKE;
            case 1 -> InteractionType.SAVE;
            case 2 -> InteractionType.VIEW;
            case 3 -> InteractionType.SHARE;
            default -> focused ? InteractionType.CLICK : InteractionType.SKIP;
        };
    }

    private InteractionEvent saveEvent(Long userId, Content content, InteractionType type, int index, int eventCount) {
        InteractionEvent event = new InteractionEvent();
        event.setUserId(userId);
        event.setContentId(content.getId());
        event.setInteractionType(type);
        int durationSeconds = content.getDurationSeconds() == null ? 60 : content.getDurationSeconds();
        event.setWatchTimeSeconds(type == InteractionType.SKIP ? 3.0 : Math.min(90.0, durationSeconds * 0.72));
        event.setDeviceType("replay-simulator");
        event.setSessionId("replay-" + System.currentTimeMillis());
        event.setTimestamp(LocalDateTime.now().minusSeconds(eventCount - index));
        return interactionEventRepository.save(event);
    }

    private InteractionEventRequest toRequest(InteractionEvent event) {
        return new InteractionEventRequest(
                event.getUserId(),
                event.getContentId(),
                event.getInteractionType(),
                event.getWatchTimeSeconds(),
                event.getTimestamp(),
                event.getDeviceType(),
                event.getSessionId()
        );
    }
}
