package com.veda.recommendation.repository;

import com.veda.recommendation.entity.ContentFeature;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentFeatureRepository extends JpaRepository<ContentFeature, Long> {
    List<ContentFeature> findAllByOrderByPopularityScoreDesc(Pageable pageable);
}
