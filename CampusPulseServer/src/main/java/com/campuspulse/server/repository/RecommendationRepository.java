package com.campuspulse.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campuspulse.server.domain.Recommendation;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByUserIdOrderByScoreDesc(Long userId);

    @Modifying
    @Query("delete from Recommendation r where r.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
