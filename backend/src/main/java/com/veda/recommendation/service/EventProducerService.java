package com.veda.recommendation.service;

import com.veda.recommendation.dto.InteractionEventRequest;
import com.veda.recommendation.entity.InteractionEvent;
import com.veda.recommendation.exception.ResourceNotFoundException;
import com.veda.recommendation.repository.ContentRepository;
import com.veda.recommendation.repository.InteractionEventRepository;
import com.veda.recommendation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventProducerService {
    private final InteractionEventRepository interactionEventRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final KafkaTemplate<String, InteractionEventRequest> kafkaTemplate;
    private final String topicName;

    public EventProducerService(
            InteractionEventRepository interactionEventRepository,
            UserRepository userRepository,
            ContentRepository contentRepository,
            KafkaTemplate<String, InteractionEventRequest> kafkaTemplate,
            @Value("${recommendation.kafka.interaction-topic}") String topicName
    ) {
        this.interactionEventRepository = interactionEventRepository;
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    @Transactional
    public InteractionEvent record(InteractionEventRequest request) {
        if (!userRepository.existsById(request.userId())) {
            throw new ResourceNotFoundException("User not found: " + request.userId());
        }
        if (!contentRepository.existsById(request.contentId())) {
            throw new ResourceNotFoundException("Content not found: " + request.contentId());
        }

        InteractionEvent event = new InteractionEvent();
        event.setUserId(request.userId());
        event.setContentId(request.contentId());
        event.setInteractionType(request.interactionType());
        event.setWatchTimeSeconds(request.watchTimeSeconds());
        event.setTimestamp(request.timestamp());
        event.setDeviceType(request.deviceType());
        event.setSessionId(request.sessionId());
        InteractionEvent saved = interactionEventRepository.save(event);

        kafkaTemplate.send(topicName, request.userId().toString(), request);
        return saved;
    }

    public List<InteractionEvent> eventsForUser(Long userId) {
        return interactionEventRepository.findByUserIdOrderByTimestampDesc(userId);
    }
}
