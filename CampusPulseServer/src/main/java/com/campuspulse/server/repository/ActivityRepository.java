package com.campuspulse.server.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campuspulse.server.domain.Activity;

import jakarta.persistence.LockModeType;

public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Activity a where a.id = :id")
    Optional<Activity> findByIdForUpdate(@Param("id") Long id);

    List<Activity> findTop200ByStatusAndStartTimeAfterOrderByStartTimeAsc(String status, LocalDateTime startTime);

    List<Activity> findTop5ByStatusAndStartTimeAfterOrderByStartTimeAsc(String status, LocalDateTime startTime);

    List<Activity> findTop5ByOrderByCreatedAtDesc();

    long countByStatus(String status);

    long countByStatusAndStartTimeAfter(String status, LocalDateTime startTime);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Activity a set a.viewCount = a.viewCount + 1 where a.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Activity a set a.shareCount = a.shareCount + 1 where a.id = :id")
    int incrementShareCount(@Param("id") Long id);

    /**
     * MySQL FULLTEXT 检索。H2 没有 MATCH AGAINST，测试环境通过
     * app.search.fulltext=false 回退到 ActivitySpecifications 的 LIKE。
     */
    @Query(value = """
            SELECT a.* FROM activity a
            WHERE a.status = 'PUBLISHED'
              AND MATCH(a.title, a.description, a.location, a.type)
                  AGAINST (:keyword IN NATURAL LANGUAGE MODE)
              AND (:tagId IS NULL OR EXISTS (
                    SELECT 1 FROM activity_tag at
                    WHERE at.activity_id = a.id AND at.tag_id = :tagId))
              AND (:type IS NULL OR a.type = :type)
              AND (:fromTime IS NULL OR a.start_time >= :fromTime)
              AND (:toTime IS NULL OR a.start_time < :toTime)
            ORDER BY a.start_time ASC, a.id ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM activity a
            WHERE a.status = 'PUBLISHED'
              AND MATCH(a.title, a.description, a.location, a.type)
                  AGAINST (:keyword IN NATURAL LANGUAGE MODE)
              AND (:tagId IS NULL OR EXISTS (
                    SELECT 1 FROM activity_tag at
                    WHERE at.activity_id = a.id AND at.tag_id = :tagId))
              AND (:type IS NULL OR a.type = :type)
              AND (:fromTime IS NULL OR a.start_time >= :fromTime)
              AND (:toTime IS NULL OR a.start_time < :toTime)
            """,
            nativeQuery = true)
    Page<Activity> searchFulltext(@Param("keyword") String keyword,
                                  @Param("tagId") Integer tagId,
                                  @Param("type") String type,
                                  @Param("fromTime") LocalDateTime fromTime,
                                  @Param("toTime") LocalDateTime toTime,
                                  Pageable pageable);
}
