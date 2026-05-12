package com.veda.recommendation.repository;

import com.veda.recommendation.entity.RecommendationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationLogRepository extends JpaRepository<RecommendationLog, Long> {
    List<RecommendationLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
