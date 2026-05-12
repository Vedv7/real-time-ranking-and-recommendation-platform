package com.veda.recommendation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_features")
public class ContentFeature {
    @Id
    private Long contentId;

    private Long totalViews = 0L;
    private Long totalLikes = 0L;
    private Long totalSkips = 0L;
    private Long totalShares = 0L;
    private Double avgWatchTime = 0.0;
    private Double popularityScore = 0.0;
    private Double freshnessScore = 1.0;
    private Double engagementRate = 0.0;
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getContentId() {
        return contentId;
    }

    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    public Long getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(Long totalViews) {
        this.totalViews = totalViews;
    }

    public Long getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(Long totalLikes) {
        this.totalLikes = totalLikes;
    }

    public Long getTotalSkips() {
        return totalSkips;
    }

    public void setTotalSkips(Long totalSkips) {
        this.totalSkips = totalSkips;
    }

    public Long getTotalShares() {
        return totalShares;
    }

    public void setTotalShares(Long totalShares) {
        this.totalShares = totalShares;
    }

    public Double getAvgWatchTime() {
        return avgWatchTime;
    }

    public void setAvgWatchTime(Double avgWatchTime) {
        this.avgWatchTime = avgWatchTime;
    }

    public Double getPopularityScore() {
        return popularityScore;
    }

    public void setPopularityScore(Double popularityScore) {
        this.popularityScore = popularityScore;
    }

    public Double getFreshnessScore() {
        return freshnessScore;
    }

    public void setFreshnessScore(Double freshnessScore) {
        this.freshnessScore = freshnessScore;
    }

    public Double getEngagementRate() {
        return engagementRate;
    }

    public void setEngagementRate(Double engagementRate) {
        this.engagementRate = engagementRate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
