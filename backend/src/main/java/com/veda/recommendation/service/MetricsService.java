package com.veda.recommendation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    public void recordFeedRequest(Long userId, int requestedLimit, int returnedItems, long latencyMs) {
        log.info("recommendation_request userId={} requestedLimit={} returnedItems={} latencyMs={}",
                userId, requestedLimit, returnedItems, latencyMs);
    }

    public void recordInferenceLatency(Long userId, int candidates, long latencyMs) {
        log.info("ranking_inference userId={} candidates={} latencyMs={}", userId, candidates, latencyMs);
    }
}
