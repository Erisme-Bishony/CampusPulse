package com.campuspulse.server.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campuspulse.server.domain.ActivityTag;
import com.campuspulse.server.domain.ActivityTagId;

public interface ActivityTagRepository extends JpaRepository<ActivityTag, ActivityTagId> {

    List<ActivityTag> findByIdActivityIdIn(Collection<Long> activityIds);

    boolean existsByIdTagId(Integer tagId);

    @Modifying
    @Query("delete from ActivityTag link where link.id.activityId = :activityId")
    void deleteByActivityId(@Param("activityId") Long activityId);
}
