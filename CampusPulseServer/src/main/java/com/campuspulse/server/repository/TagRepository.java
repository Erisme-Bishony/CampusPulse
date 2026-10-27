package com.campuspulse.server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campuspulse.server.domain.Tag;

public interface TagRepository extends JpaRepository<Tag, Integer> {

    List<Tag> findAllByOrderBySortOrderAscIdAsc();

    Optional<Tag> findByName(String name);

    boolean existsByName(String name);
}
