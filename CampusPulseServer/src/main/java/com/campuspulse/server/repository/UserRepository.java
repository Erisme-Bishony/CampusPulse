package com.campuspulse.server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campuspulse.server.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
