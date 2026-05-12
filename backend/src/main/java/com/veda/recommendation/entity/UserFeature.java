package com.veda.recommendation.entity;

import com.veda.recommendation.enums.ContentCategory;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_features")
public class UserFeature {
    @Id
    private Long userId;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private Set<ContentCategory> preferredCategories = new HashSet<>();

    private Double avgWatchTime = 0.0;
    private Double likeRate = 0.0;
    private Double skipRate = 0.0;
    private Double shareRate = 0.0;
    private Long totalInteractions = 0L;
    private LocalDateTime lastActiveAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Set<ContentCategory> getPreferredCategories() {
        return preferredCategories;
    }

    public void setPreferredCategories(Set<ContentCategory> preferredCategories) {
        this.preferredCategories = preferredCategories;
    }

    public Double getAvgWatchTime() {
        return avgWatchTime;
    }

    public void setAvgWatchTime(Double avgWatchTime) {
        this.avgWatchTime = avgWatchTime;
    }

    public Double getLikeRate() {
        return likeRate;
    }

    public void setLikeRate(Double likeRate) {
        this.likeRate = likeRate;
    }

    public Double getSkipRate() {
        return skipRate;
    }

    public void setSkipRate(Double skipRate) {
        this.skipRate = skipRate;
    }

    public Double getShareRate() {
        return shareRate;
    }

    public void setShareRate(Double shareRate) {
        this.shareRate = shareRate;
    }

    public Long getTotalInteractions() {
        return totalInteractions;
    }

    public void setTotalInteractions(Long totalInteractions) {
        this.totalInteractions = totalInteractions;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
