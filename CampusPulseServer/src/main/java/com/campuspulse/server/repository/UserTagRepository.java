package com.campuspulse.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campuspulse.server.domain.UserTag;
import com.campuspulse.server.domain.UserTagId;

public interface UserTagRepository extends JpaRepository<UserTag, UserTagId> {

    List<UserTag> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
