package com.veda.recommendation.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class MetricsService {
    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final MeterRegistry meterRegistry;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordFeedRequest(Long userId, int requestedLimit, int returnedItems, long latencyMs, String policy, String modelVersion) {
        meterRegistry.counter("recommendation.feed.requests", "policy", policy, "model_version", modelVersion).increment();
        Timer.builder("recommendation.feed.latency")
                .tag("policy", policy)
                .tag("model_version", modelVersion)
                .register(meterRegistry)
                .record(Duration.ofMillis(latencyMs));
        log.info("recommendation_request userId={} requestedLimit={} returnedItems={} latencyMs={} policy={} modelVersion={}",
                userId, requestedLimit, returnedItems, latencyMs, policy, modelVersion);
    }

    public void recordCacheHit(String cacheName) {
        meterRegistry.counter("recommendation.cache.hit", "cache", cacheName).increment();
    }

    public void recordCacheMiss(String cacheName) {
        meterRegistry.counter("recommendation.cache.miss", "cache", cacheName).increment();
    }

    public void recordInferenceLatency(Long userId, int candidates, long latencyMs, String modelVersion) {
        Timer.builder("recommendation.ml.inference.latency")
                .tag("model_version", modelVersion)
                .register(meterRegistry)
                .record(Duration.ofMillis(latencyMs));
        log.info("ranking_inference userId={} candidates={} latencyMs={} modelVersion={}", userId, candidates, latencyMs, modelVersion);
    }
}
