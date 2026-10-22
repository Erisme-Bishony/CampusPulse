package com.campuspulse.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campuspulse.server.domain.Tag;

public interface TagRepository extends JpaRepository<Tag, Integer> {

    List<Tag> findAllByOrderBySortOrderAscIdAsc();
}
