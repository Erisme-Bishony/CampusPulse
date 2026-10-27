package com.campuspulse.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campuspulse.server.domain.Behavior;

public interface BehaviorRepository extends JpaRepository<Behavior, Long> {
}
