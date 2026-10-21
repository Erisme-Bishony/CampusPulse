package com.campuspulse.server.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campuspulse.server.domain.ActivityTag;
import com.campuspulse.server.domain.ActivityTagId;

public interface ActivityTagRepository extends JpaRepository<ActivityTag, ActivityTagId> {

    List<ActivityTag> findByIdActivityIdIn(Collection<Long> activityIds);
}
