package com.veda.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_logs")
public class RecommendationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long contentId;
    private Double predictedCtr;
    private Double finalScore;
    private Integer rankPosition;
    private String modelVersion;
    private String rankingPolicy;
    private String experimentBucket;

    @Column(columnDefinition = "TEXT")
    private String featureSnapshotJson;

    private Long latencyMs;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getContentId() {
        return contentId;
    }

    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    public Double getPredictedCtr() {
        return predictedCtr;
    }

    public void setPredictedCtr(Double predictedCtr) {
        this.predictedCtr = predictedCtr;
    }

    public Double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Double finalScore) {
        this.finalScore = finalScore;
    }

    public Integer getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(Integer rankPosition) {
        this.rankPosition = rankPosition;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getRankingPolicy() {
        return rankingPolicy;
    }

    public void setRankingPolicy(String rankingPolicy) {
        this.rankingPolicy = rankingPolicy;
    }

    public String getExperimentBucket() {
        return experimentBucket;
    }

    public void setExperimentBucket(String experimentBucket) {
        this.experimentBucket = experimentBucket;
    }

    public String getFeatureSnapshotJson() {
        return featureSnapshotJson;
    }

    public void setFeatureSnapshotJson(String featureSnapshotJson) {
        this.featureSnapshotJson = featureSnapshotJson;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
