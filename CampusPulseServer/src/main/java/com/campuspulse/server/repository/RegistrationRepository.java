package com.campuspulse.server.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campuspulse.server.domain.Registration;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    long countByStatus(String status);

    long countByCreatedAtAfter(LocalDateTime time);

    Optional<Registration> findByUserIdAndActivityId(Long userId, Long activityId);

    List<Registration> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Registration> findByUserIdAndStatus(Long userId, String status);
}
