package com.campuspulse.server.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Activity a set a.viewCount = a.viewCount + 1 where a.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Activity a set a.shareCount = a.shareCount + 1 where a.id = :id")
    int incrementShareCount(@Param("id") Long id);
}
