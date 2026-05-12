package com.veda.recommendation.service;

import com.veda.recommendation.dto.InteractionEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EventConsumerService {
    private static final Logger log = LoggerFactory.getLogger(EventConsumerService.class);

    private final FeatureUpdateService featureUpdateService;

    public EventConsumerService(FeatureUpdateService featureUpdateService) {
        this.featureUpdateService = featureUpdateService;
    }

    @KafkaListener(topics = "${recommendation.kafka.interaction-topic}", groupId = "recommendation-feature-updater")
    public void consume(InteractionEventRequest event) {
        log.info("Consumed interaction event userId={} contentId={} type={}",
                event.userId(), event.contentId(), event.interactionType());
        featureUpdateService.updateFeatures(event);
    }
}
