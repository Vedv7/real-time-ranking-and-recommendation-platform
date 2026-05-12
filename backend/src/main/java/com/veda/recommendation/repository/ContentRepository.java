package com.veda.recommendation.repository;

import com.veda.recommendation.entity.Content;
import com.veda.recommendation.enums.ContentCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {
    List<Content> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAt, Pageable pageable);

    List<Content> findByCategoryIn(Collection<ContentCategory> categories, Pageable pageable);
}
