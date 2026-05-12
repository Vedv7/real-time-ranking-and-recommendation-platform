package com.veda.recommendation.repository;

import com.veda.recommendation.entity.UserFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface UserFeatureRepository extends JpaRepository<UserFeature, Long> {
    long countByUpdatedAtBefore(LocalDateTime threshold);
}
