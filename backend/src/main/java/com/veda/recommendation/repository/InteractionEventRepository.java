package com.veda.recommendation.repository;

import com.veda.recommendation.entity.InteractionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface InteractionEventRepository extends JpaRepository<InteractionEvent, Long> {
    List<InteractionEvent> findByUserIdOrderByTimestampDesc(Long userId);

    List<InteractionEvent> findByUserIdOrderByTimestampAsc(Long userId);

    @Query("select distinct e.contentId from InteractionEvent e where e.userId = :userId")
    Set<Long> findInteractedContentIds(Long userId);
}
