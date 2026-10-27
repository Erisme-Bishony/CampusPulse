package com.campuspulse.server.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campuspulse.server.domain.Behavior;

public interface BehaviorRepository extends JpaRepository<Behavior, Long> {

    long countByCreatedAtAfter(LocalDateTime time);
}
